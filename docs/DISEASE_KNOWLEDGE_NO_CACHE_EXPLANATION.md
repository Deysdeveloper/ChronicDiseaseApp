# Disease Knowledge Feature - No Cache Implementation

## Problem Statement

**Question:** "Will new users on refresh fetch old values?"

**Answer:** **NO** - The implementation ensures that new users (and all users) always fetch fresh
data from the server.

---

## How We Prevent Cached Data Issues

### 1. **Force Server Fetch with `Source.SERVER`**

The key to preventing stale cached data is using Firestore's `Source.SERVER` parameter:

```kotlin
// In DiseaseKnowledgeRepository.kt
val snapshot = articlesCollection
    .orderBy("publishedDate", Query.Direction.DESCENDING)
    .get(Source.SERVER)  // ← This forces server fetch, bypasses local cache
    .await()
```

### What This Does:

- **`Source.SERVER`**: Forces Firestore to fetch data directly from the Firebase server
- **Bypasses local cache**: Ignores any locally cached data
- **Always fresh**: Every fetch gets the latest data from the database

### Without `Source.SERVER`:

By default, Firestore uses offline persistence and may return cached data, which could show old
articles to new users.

---

## 2. **No In-Memory Caching in Repository**

The repository does NOT store articles in memory:

```kotlin
class DiseaseKnowledgeRepository {
    // No cache variables like:
    // private var cachedArticles: List<DiseaseArticle>? = null
    
    // Every call fetches fresh data:
    fun getDiseaseArticles(): Flow<Result<List<DiseaseArticle>>> = flow {
        // Fresh fetch every time
        val snapshot = articlesCollection
            .get(Source.SERVER)
            .await()
        // ...
    }
}
```

---

## 3. **ViewModel State Management**

The ViewModel manages UI state but doesn't cache data across sessions:

```kotlin
class DiseaseKnowledgeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DiseaseKnowledgeState())
    
    fun fetchDiseaseKnowledge() {
        viewModelScope.launch {
            // Always calls repository, which fetches from server
            repository.getDiseaseArticles().collect { result ->
                // Updates state with fresh data
            }
        }
    }
    
    fun refresh() {
        // Explicitly re-fetches data
        fetchDiseaseKnowledge()
    }
}
```

**Important Notes:**

- ViewModel state is only for the current session
- When the app is restarted or the ViewModel is recreated, state resets
- No data is persisted across app restarts

---

## 4. **Pull-to-Refresh Implementation**

The UI provides pull-to-refresh functionality:

```kotlin
PullToRefreshBox(
    isRefreshing = uiState.isLoading,
    onRefresh = { viewModel.refresh() },  // ← Fetches fresh data
    modifier = Modifier.fillMaxSize()
) {
    // Article list
}
```

Users can manually refresh to get the latest articles at any time.

---

## User Scenarios

### Scenario 1: New User Opens App

1. User installs app
2. User navigates to Disease Knowledge screen
3. ViewModel calls `fetchDiseaseKnowledge()`
4. Repository fetches from server with `Source.SERVER`
5. **Result:** Fresh, up-to-date articles displayed

### Scenario 2: Existing User Returns After Days

1. User opens app after several days
2. Disease Knowledge screen loads
3. ViewModel fetches data
4. Repository uses `Source.SERVER` (not cache)
5. **Result:** Latest articles from server, not old cached data

### Scenario 3: User Pulls to Refresh

1. User swipes down on article list
2. `viewModel.refresh()` is called
3. Fresh data fetched from server
4. **Result:** Guaranteed latest articles

### Scenario 4: Network Failure

1. User tries to load articles
2. Server fetch fails (no network)
3. Error state displayed with retry button
4. **Result:** User knows data couldn't be fetched (no misleading cached data)

---

## Comparison: With vs Without Cache Prevention

| Aspect | **Without `Source.SERVER`** (Default) | **With `Source.SERVER`** (Our Implementation) |
|--------|--------------------------------------|---------------------------------------------|
| First load | May show cached data if available | Always fetches from server |
| After app restart | May show stale cached data | Fetches fresh data |
| New user | Could see another user's cached data | Always gets fresh server data |
| Data freshness | Unpredictable | Guaranteed fresh on every fetch |
| Offline behavior | Shows cached data | Shows error (user knows it's not fresh) |

---

## Code Flow Diagram

```
User Opens Screen
       ↓
ViewModel.init() calls fetchDiseaseKnowledge()
       ↓
Repository.getDiseaseArticles()
       ↓
Firestore Query with Source.SERVER
       ↓
Network Request to Firebase Server
       ↓
Fresh Data Returned
       ↓
UI Updates with Latest Articles
```

---

## Additional Features

### 1. **Category Filtering**

Users can filter by category, and each filter fetches fresh data:

```kotlin
fun filterByCategory(category: String?) {
    fetchDiseaseKnowledge(category)  // Fresh fetch for filtered data
}
```

### 2. **Article Detail View**

Individual articles also fetch fresh data:

```kotlin
suspend fun getArticleById(articleId: String): Result<DiseaseArticle?> {
    val doc = articlesCollection
        .document(articleId)
        .get(Source.SERVER)  // Fresh data for detail view too
        .await()
    // ...
}
```

---

## Testing the Implementation

### Test 1: Fresh Data on Load

1. Add new article to Firebase
2. Open app (or refresh screen)
3. **Expected:** New article appears immediately

### Test 2: No Stale Cache

1. View articles on one device
2. Add new article on another device
3. Pull to refresh on first device
4. **Expected:** New article appears

### Test 3: Network Error Handling

1. Turn off internet
2. Try to load articles
3. **Expected:** Error message, no misleading cached data

---

## Configuration

If you ever need to change caching behavior, modify the `Source` parameter:

```kotlin
// Current (no cache):
.get(Source.SERVER)

// Alternative options:
.get(Source.CACHE)     // Only local cache (not recommended for this use case)
.get(Source.DEFAULT)   // Server first, then cache (may show stale data)
```

**Our recommendation:** Keep `Source.SERVER` for Disease Knowledge articles to ensure users always
see the latest health information.

---

## Summary

✅ **New users will NOT see old cached data**  
✅ **Every fetch gets fresh data from server**  
✅ **Pull-to-refresh guarantees latest articles**  
✅ **No in-memory or persistent caching**  
✅ **Network errors are clearly communicated**

The implementation prioritizes data freshness over speed, which is appropriate for health-related
content where accuracy is critical.
