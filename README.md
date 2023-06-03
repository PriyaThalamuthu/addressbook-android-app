# [Addressbook](https://github.com/dredwardhyde/addressbook) Android Client Application

[![medium](https://aleen42.github.io/badges/src/medium.svg)](https://medium.com/geekculture/how-to-make-a-login-activity-with-biometrics-support-on-android-62185f19cda1)  [![medium](https://aleen42.github.io/badges/src/medium.svg)](https://medium.com/geekculture/how-to-build-sign-and-publish-android-application-using-github-actions-aa6346679254)
[![Android CI](https://github.com/dredwardhyde/addressbook-android-app/actions/workflows/android.yml/badge.svg)](https://github.com/dredwardhyde/addressbook-android-app/actions/workflows/android.yml)

### Features

- **[Biometrics support](https://github.com/dredwardhyde/addressbook-android-app/blob/master/app/src/main/kotlin/com/deepschneider/addressbook/activities/LoginActivity.kt#L328)**
- **[Instrumentation testing with Espresso](https://github.com/dredwardhyde/addressbook-android-app/blob/master/app/src/androidTest/kotlin/com/deepschneider/addressbook/WorkflowTest.kt)**
- **[Dark/Light Theme support](https://github.com/dredwardhyde/addressbook-android-app/blob/master/app/src/main/res/values/themes.xml) with [dynamic switching](https://github.com/dredwardhyde/addressbook-android-app/blob/master/app/src/main/kotlin/com/deepschneider/addressbook/activities/LoginActivity.kt#L60)**
- **[Dynamic Dark/Light icon switching](https://github.com/dredwardhyde/addressbook-android-app/blob/master/app/src/main/kotlin/com/deepschneider/addressbook/activities/LoginActivity.kt#L106)**
- **Downloading files using [DownloadManager](https://github.com/dredwardhyde/addressbook-android-app/blob/master/app/src/main/kotlin/com/deepschneider/addressbook/adapters/DocumentsListAdapter.kt#L62)**  
- **Receiving download status using [BroadcastReceiver](https://github.com/dredwardhyde/addressbook-android-app/blob/master/app/src/main/kotlin/com/deepschneider/addressbook/receivers/DownloadBroadcastReceiver.kt#L22)**  
- **[Multipart file uploading](https://github.com/dredwardhyde/addressbook-android-app/blob/master/app/src/main/kotlin/com/deepschneider/addressbook/activities/CreateOrEditPersonActivity.kt#L251) using Retrofit2 with [custom progress tracking](https://github.com/dredwardhyde/addressbook-android-app/blob/master/app/src/main/kotlin/com/deepschneider/addressbook/network/ProgressRequestBody.kt) and custom [ProgressDialog](https://github.com/dredwardhyde/addressbook-android-app/blob/master/app/src/main/res/layouts/dialogs/layout/uploading_progress_dialog.xml)**
- **[User certificates support](https://github.com/dredwardhyde/addressbook-android-app/blob/master/app/src/main/res/xml/network_security_config.xml#L5)**  
- **[Building](https://github.com/dredwardhyde/addressbook-android-app/blob/master/.github/workflows/android.yml#L31), [signing](https://github.com/dredwardhyde/addressbook-android-app/blob/master/app/build.gradle#L20) and [publishing release](https://github.com/dredwardhyde/addressbook-android-app/blob/master/.github/workflows/android.yml#L44) using Github Actions**  
- **[Custom requests with JWT authentication](https://github.com/dredwardhyde/addressbook-android-app/blob/master/app/src/main/kotlin/com/deepschneider/addressbook/network/FilteredListRequest.kt) using [Android Volley](https://github.com/dredwardhyde/addressbook-android-app/blob/master/app/src/main/kotlin/com/deepschneider/addressbook/activities/AbstractListActivity.kt#L100)**  
- **[Server-side pagination and filtering](https://github.com/dredwardhyde/addressbook-android-app/blob/master/app/src/main/kotlin/com/deepschneider/addressbook/activities/OrganizationsActivity.kt#L86)**  
- **[Custom list animation](https://github.com/dredwardhyde/addressbook-android-app/blob/master/app/src/main/kotlin/com/deepschneider/addressbook/activities/AbstractListActivity.kt#L119)**  
- **[Custom gesture recognition](https://github.com/dredwardhyde/addressbook-android-app/blob/master/app/src/main/kotlin/com/deepschneider/addressbook/listeners/OnSwipeTouchListener.kt)**  
- **[ProGuard support](https://github.com/dredwardhyde/addressbook-android-app/blob/master/app/proguard-rules.pro)**  

### Light Theme
<img src="https://raw.githubusercontent.com/dredwardhyde/addressbook-android-app/master/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="70"/>  
<img src="https://raw.githubusercontent.com/dredwardhyde/addressbook-android-app/master/screenshots/all_panels_light.png" width="1000"/>  

### Dark Theme
<img src="https://raw.githubusercontent.com/dredwardhyde/addressbook-android-app/master/app/src/main/res/mipmap-xxxhdpi/ic_launcher_dark.png" width="70"/>  
<img src="https://raw.githubusercontent.com/dredwardhyde/addressbook-android-app/master/screenshots/all_panels_dark.png" width="1000"/>  


### Demo
[![Video](https://img.youtube.com/vi/7J0j0lTKfNg/maxresdefault.jpg)](https://www.youtube.com/watch?v=7J0j0lTKfNg)
