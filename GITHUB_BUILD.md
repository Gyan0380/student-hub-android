# GitHub Android Build

1. Upload/push this project to GitHub.
2. Open the repository's **Actions** tab.
3. Select **Android Build**.
4. Click **Run workflow** (or push a commit to trigger it).
5. When the workflow finishes, open the run and download the **student-hub-debug-apk** artifact.

The workflow installs Gradle 8.9, uses JDK 17, and builds the debug APK without requiring a committed `gradle-wrapper.jar`.
