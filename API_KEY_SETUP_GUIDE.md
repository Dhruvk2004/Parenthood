# Google Maps API Key Setup Guide

## ✅ What I've Done

I've secured your Google Maps API key using Android best practices. Here's what changed:

### 1. **Centralized API Key Storage**
- **Location:** `local.properties` (root directory)
- **Key:** `MAPS_API_KEY=AIzaSyD70Xp36MbJ3aREpZYDHmcbgAXar4-c4SM`
- **Security:** This file is already in `.gitignore` and won't be committed to Git

### 2. **Removed Hardcoded Keys**
✅ Removed from `AndroidManifest.xml` - now uses `${MAPS_API_KEY}` placeholder
✅ Removed from `strings.xml` - now has placeholder text
✅ Removed from `MapFragment.kt` - now uses `BuildConfig.MAPS_API_KEY`
✅ Removed from `HomeFragment.kt` - now uses `BuildConfig.MAPS_API_KEY`

### 3. **Build Configuration**
Updated `app/build.gradle.kts` to:
- Enable `buildConfig = true`
- Read API key from `local.properties`
- Generate `BuildConfig.MAPS_API_KEY` constant
- Inject key into AndroidManifest at build time

---

## 🔑 How to Update Your API Key (Single Place)

**You only need to update ONE file:**

```properties
# File: local.properties (in project root)

MAPS_API_KEY=YOUR_NEW_API_KEY_HERE
```

That's it! The key will automatically be used everywhere in your app.

---

## 🆕 How to Generate a New API Key

### Step 1: Go to Google Cloud Console
1. Visit: https://console.cloud.google.com/
2. Select your project: `fir-a9091` (or create a new one)

### Step 2: Enable Required APIs
Go to **APIs & Services > Library** and enable:
- ✅ Maps SDK for Android
- ✅ Places API
- ✅ Geocoding API
- ✅ Geolocation API

### Step 3: Create API Key
1. Go to **APIs & Services > Credentials**
2. Click **+ CREATE CREDENTIALS > API key**
3. Copy the generated key

### Step 4: Restrict the API Key (IMPORTANT!)
1. Click on your new API key to edit it
2. Under **Application restrictions**:
   - Select **Android apps**
   - Click **+ Add an item**
   - Add your package name: `com.example.boardingscreen`
   - Add your SHA-1 fingerprint (see below)

3. Under **API restrictions**:
   - Select **Restrict key**
   - Check these APIs:
     - Maps SDK for Android
     - Places API
     - Geocoding API
     - Geolocation API

4. Click **Save**

### Step 5: Get Your SHA-1 Fingerprint

**For Debug Build:**
```bash
cd android
./gradlew signingReport
```

Or use keytool:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

**For Release Build:**
```bash
keytool -list -v -keystore /path/to/your/release.keystore -alias your_alias
```

Copy the SHA-1 fingerprint and add it to your API key restrictions.

### Step 6: Update local.properties
```properties
MAPS_API_KEY=YOUR_NEW_API_KEY_HERE
```

### Step 7: Sync & Build
1. In Android Studio: **File > Sync Project with Gradle Files**
2. Clean and rebuild: **Build > Clean Project** then **Build > Rebuild Project**

---

## 🔒 Security Checklist

- ✅ API key is in `local.properties` (not committed to Git)
- ✅ `local.properties` is in `.gitignore`
- ✅ No hardcoded keys in source code
- ✅ Using `BuildConfig` for compile-time injection
- ⚠️ **TODO:** Regenerate API key (current one is exposed in Git history)
- ⚠️ **TODO:** Add API restrictions in Google Cloud Console
- ⚠️ **TODO:** Add SHA-1 fingerprint restrictions

---

## 📝 How It Works

### Build Time
1. Gradle reads `MAPS_API_KEY` from `local.properties`
2. Generates `BuildConfig.MAPS_API_KEY` constant
3. Injects key into `AndroidManifest.xml` via `${MAPS_API_KEY}` placeholder

### Runtime
- **MapFragment.kt:** Uses `BuildConfig.MAPS_API_KEY` to initialize Places API
- **HomeFragment.kt:** Uses `BuildConfig.MAPS_API_KEY` for Geocoding API
- **AndroidManifest.xml:** Key is injected at build time for Maps SDK

### All Usage Points (Automatic)
```kotlin
// MapFragment.kt - Line 210
Places.initialize(requireContext(), BuildConfig.MAPS_API_KEY)

// HomeFragment.kt - Line 456
val geoApiContext = GeoApiContext.Builder()
    .apiKey(BuildConfig.MAPS_API_KEY)
    .build()

// AndroidManifest.xml - Line 91
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="${MAPS_API_KEY}" />
```

---

## 🚨 Important Notes

### For Team Members
Each developer needs to add the API key to their own `local.properties`:
```properties
MAPS_API_KEY=the_shared_api_key
```

### For CI/CD
Add the API key as an environment variable:
```bash
export MAPS_API_KEY=your_api_key
```

Or inject it during build:
```bash
./gradlew assembleRelease -PMAPS_API_KEY=your_api_key
```

### Git History Cleanup (Optional but Recommended)
Your old API key is still in Git history. To remove it:

```bash
# WARNING: This rewrites Git history - coordinate with team!
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch app/src/main/res/values/strings.xml" \
  --prune-empty --tag-name-filter cat -- --all

# Force push (be careful!)
git push origin --force --all
```

**Easier approach:** Just regenerate the API key and restrict it properly.

---

## ✅ Verification

After setup, verify everything works:

1. **Build succeeds:**
   ```bash
   ./gradlew clean build
   ```

2. **Check BuildConfig:**
   ```bash
   # After building, check generated file:
   cat app/build/generated/source/buildConfig/debug/com/example/boardingscreen/BuildConfig.java
   ```
   Should contain: `public static final String MAPS_API_KEY = "your_key";`

3. **Run the app:**
   - Open MapFragment - map should load
   - Search for places - autocomplete should work
   - Check HomeFragment - location should reverse geocode

---

## 🆘 Troubleshooting

### "BuildConfig.MAPS_API_KEY cannot be resolved"
- Sync Gradle: **File > Sync Project with Gradle Files**
- Clean build: **Build > Clean Project**
- Rebuild: **Build > Rebuild Project**

### "Map not loading" or "Places API not working"
- Check `local.properties` has the correct key
- Verify APIs are enabled in Google Cloud Console
- Check API key restrictions allow your package name and SHA-1

### "API key not found in local.properties"
- Make sure file exists in project root (same level as `settings.gradle.kts`)
- Check the property name is exactly `MAPS_API_KEY` (case-sensitive)
- No spaces around the `=` sign

---

## 📚 Additional Resources

- [Google Maps Platform - Get API Key](https://developers.google.com/maps/documentation/android-sdk/get-api-key)
- [API Key Best Practices](https://developers.google.com/maps/api-security-best-practices)
- [Secrets Gradle Plugin](https://github.com/google/secrets-gradle-plugin)

---

**Status:** ✅ API key is now secured and centralized in `local.properties`

**Next Step:** Regenerate the API key in Google Cloud Console and update `local.properties`
