# Firebase Setup for Disease Knowledge Articles

## Firestore Collection Structure

### Collection Name: `diseaseArticles`

Each document in the `diseaseArticles` collection should have the following structure:

---

## Document Fields

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `title` | String | Yes | Article title | "Understanding Type 2 Diabetes" |
| `content` | String | Yes | Full article content | "Type 2 diabetes is a chronic condition..." |
| `imageUrl` | String | No | URL to article image | "https://example.com/diabetes.jpg" |
| `category` | String | Yes | Article category | "Diabetes", "Heart Disease", "Hypertension" |
| `author` | String | No | Article author name | "Dr. Jane Smith" |
| `publishedDate` | Number (Timestamp) | Yes | Publication date in milliseconds | 1703289600000 |
| `tags` | Array of Strings | No | Article tags | ["diabetes", "blood sugar", "prevention"] |

---

## Example Document 1: Diabetes Article

```json
{
  "title": "Understanding Type 2 Diabetes",
  "content": "Type 2 diabetes is a chronic condition that affects the way your body metabolizes sugar (glucose). With type 2 diabetes, your body either resists the effects of insulin or doesn't produce enough insulin to maintain normal glucose levels.\n\nSymptoms include increased thirst, frequent urination, hunger, fatigue, and blurred vision. While there's no cure for type 2 diabetes, managing your condition through healthy eating, regular exercise, and maintaining a healthy weight can help.",
  "imageUrl": "https://images.unsplash.com/photo-1505751172876-fa1923c5c528?w=800",
  "category": "Diabetes",
  "author": "Dr. Sarah Johnson",
  "publishedDate": 1703289600000,
  "tags": ["diabetes", "type2", "blood-sugar", "prevention"]
}
```

**Document ID:** Auto-generated or custom (e.g., `diabetes_type2_understanding`)

---

## Example Document 2: Heart Disease Article

```json
{
  "title": "Heart Disease Prevention: A Complete Guide",
  "content": "Heart disease describes a range of conditions that affect your heart. The most common type is coronary artery disease, which can lead to heart attack.\n\nPrevention strategies include:\n• Eating a healthy diet rich in fruits and vegetables\n• Regular physical activity (at least 30 minutes daily)\n• Maintaining a healthy weight\n• Not smoking\n• Managing stress\n• Getting regular health screenings\n\nEarly detection and lifestyle changes can significantly reduce your risk of heart disease.",
  "imageUrl": "https://images.unsplash.com/photo-1628348068343-c6a848d2b6dd?w=800",
  "category": "Heart Disease",
  "author": "Dr. Michael Chen",
  "publishedDate": 1703376000000,
  "tags": ["heart", "prevention", "cardiovascular", "healthy-living"]
}
```

---

## Example Document 3: Hypertension Article

```json
{
  "title": "Managing High Blood Pressure Naturally",
  "content": "High blood pressure (hypertension) is a common condition that can lead to serious health problems if left untreated. The good news is that lifestyle changes can make a significant difference.\n\nNatural management strategies:\n• Reduce sodium intake (aim for less than 2,300mg daily)\n• Exercise regularly\n• Maintain healthy weight\n• Limit alcohol consumption\n• Reduce stress through meditation or yoga\n• Get adequate sleep (7-9 hours nightly)\n\nAlways consult with your healthcare provider before making major changes to your health routine.",
  "imageUrl": "https://images.unsplash.com/photo-1584362917165-526a968579e8?w=800",
  "category": "Hypertension",
  "author": "Dr. Emily Rodriguez",
  "publishedDate": 1703462400000,
  "tags": ["hypertension", "blood-pressure", "lifestyle", "prevention"]
}
```

---

## Example Document 4: General Wellness Article

```json
{
  "title": "10 Daily Habits for Chronic Disease Prevention",
  "content": "Preventing chronic diseases starts with daily habits. Here are 10 evidence-based practices:\n\n1. Eat 5-7 servings of fruits and vegetables daily\n2. Stay hydrated (8 glasses of water)\n3. Move your body for at least 30 minutes\n4. Get quality sleep\n5. Practice stress management\n6. Avoid tobacco products\n7. Limit alcohol intake\n8. Maintain social connections\n9. Schedule regular health check-ups\n10. Keep a positive mindset\n\nSmall, consistent changes lead to significant long-term health benefits.",
  "imageUrl": "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=800",
  "category": "General Wellness",
  "author": "Dr. David Williams",
  "publishedDate": 1703548800000,
  "tags": ["wellness", "prevention", "healthy-habits", "lifestyle"]
}
```

---

## Setting Up in Firebase Console

### Step 1: Create Collection

1. Go to Firebase Console → Firestore Database
2. Click "Start collection"
3. Collection ID: `diseaseArticles`
4. Click "Next"

### Step 2: Add Documents

1. Click "Add document"
2. Leave "Document ID" as "Auto-ID" (or enter custom ID)
3. Add fields as shown in examples above
4. Click "Save"

### Step 3: Repeat for Multiple Articles

Add at least 5-10 articles for a good user experience.

---

## Categories You Can Use

Here are suggested categories for organizing articles:

- **Diabetes**
- **Heart Disease**
- **Hypertension**
- **Chronic Kidney Disease**
- **COPD** (Chronic Obstructive Pulmonary Disease)
- **Asthma**
- **Arthritis**
- **General Wellness**
- **Nutrition**
- **Mental Health**
- **Exercise & Fitness**

---

## Generating Timestamps

### JavaScript (for Firebase Console)

```javascript
Date.now()  // Current timestamp in milliseconds
```

### Kotlin (for testing)

```kotlin
System.currentTimeMillis()  // Current timestamp
```

### Converting Date to Timestamp

```javascript
// December 22, 2023
new Date('2023-12-22').getTime()  // 1703203200000
```

---

## Firestore Security Rules

Add these security rules to control access:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow all authenticated users to read articles
    match /diseaseArticles/{articleId} {
      allow read: if request.auth != null;
      
      // Only admins can write (add your admin check here)
      allow write: if request.auth != null && 
                      get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }
  }
}
```

For development, you can use permissive rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /diseaseArticles/{articleId} {
      allow read: if true;  // Allow all reads
      allow write: if request.auth != null;  // Require authentication for writes
    }
  }
}
```

---

## Indexing for Better Performance

Create a composite index for efficient queries:

1. Go to Firebase Console → Firestore Database → Indexes
2. Click "Create Index"
3. Collection: `diseaseArticles`
4. Fields to index:
    - `category` (Ascending)
    - `publishedDate` (Descending)
5. Query scope: Collection
6. Click "Create"

This index supports the query:

```kotlin
articlesCollection
    .whereEqualTo("category", category)
    .orderBy("publishedDate", Query.Direction.DESCENDING)
```

---

## Testing Your Setup

### Test Query in Firebase Console

1. Go to Firestore Database
2. Click on `diseaseArticles` collection
3. Verify documents are present
4. Check all required fields are populated

### Test in App

1. Run the app
2. Navigate to Disease Knowledge screen
3. Verify articles load
4. Test pull-to-refresh
5. Click on an article to view details

---

## Sample Image URLs (Free Stock Photos)

You can use these free image URLs for testing:

- **Diabetes**: `https://images.unsplash.com/photo-1505751172876-fa1923c5c528?w=800`
- **Heart**: `https://images.unsplash.com/photo-1628348068343-c6a848d2b6dd?w=800`
- **Blood Pressure**: `https://images.unsplash.com/photo-1584362917165-526a968579e8?w=800`
- **Exercise**: `https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=800`
- **Healthy Food**: `https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=800`
- **Doctor**: `https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=800`

---

## Maintenance Tips

1. **Regular Updates**: Add new articles monthly to keep content fresh
2. **Content Review**: Review and update existing articles quarterly
3. **User Feedback**: Add a feedback mechanism to improve content
4. **Analytics**: Track which articles are most viewed
5. **Categories**: Expand categories based on user needs

---

## Quick Setup Script (Node.js)

If you prefer to add documents programmatically:

```javascript
const admin = require('firebase-admin');

// Initialize Firebase Admin
admin.initializeApp();
const db = admin.firestore();

const articles = [
  {
    title: "Understanding Type 2 Diabetes",
    content: "...",
    category: "Diabetes",
    publishedDate: Date.now(),
    // ... other fields
  },
  // Add more articles
];

async function addArticles() {
  for (const article of articles) {
    await db.collection('diseaseArticles').add(article);
    console.log('Added:', article.title);
  }
}

addArticles();
```

---

## Summary Checklist

- ✅ Create `diseaseArticles` collection in Firestore
- ✅ Add at least 5 articles with all required fields
- ✅ Set up security rules
- ✅ Create composite index (category + publishedDate)
- ✅ Test queries in Firebase Console
- ✅ Verify app displays articles correctly
- ✅ Test pull-to-refresh functionality
- ✅ Test article detail view

Your Disease Knowledge feature is now ready to use!
