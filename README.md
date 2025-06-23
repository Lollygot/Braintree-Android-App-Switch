# Braintree-Android-App-Switch

This is a demo of app switch for Android.

> **_NOTE_**: The code has been defined to my server-side schema, endpoint names, and uses my MAID naming. You will likely have to use my own server-side code which can be found here: https://github.com/Lollygot/PPCP-BT-Demo.

> _Note_: This code and setup has only been tested on Windows using the Android Studio "Medium Phone" API 35 emulator

## Configuring Android Studio

If you've never used Android Studio, then there's some configuration needed to set up the JDK and emulator

1. Open this folder in Android Studio.
2. Follow these steps to setup the JDK and an emulator (under section "Setting up your development environment"): https://paypal.atlassian.net/wiki/spaces/~615438c064ff010071026d77/pages/1065215265/Braintree+native+SDK+implementation+guide+for+Android#Setting-up-your-development-environment.
3. On the top bar, click the run button.
4. This should install this demo app onto the emulator and run it.
5. The demo app won't work until you've done the remaining emulator setup below but it's a good idea to put this demo app onto your emulator's home screen (the app is called Braintree and has the default Android logo as its icon).

## Emulator Setup

There is a lot of configuration needed to setup your emulator before app switch will work.

### Connecting to Internet

There is some configuration needed on the emulator due to corporate restrictions to allow it to connect to the internet.

1. Follow these steps to install a certificate onto the emulator (under section "Emulator changes"): https://paypal.atlassian.net/wiki/spaces/Checkout/pages/1129122168/Android+Stage+Testing+instructions+for+BT+Demo+app+and+In+App+Checkout+in+Venice#Emulator-changes.
2. Go to your internet settings on your emulator and connect to the "AndroidWifi" internet.
3. Verify that it has worked by opening Chrome on your emulator and visiting a website (e.g. https://www.wikipedia.org/). You shouldn't get any warnings about an insecure connection and should just be able to visit it as normal.

### Getting the PayPal Sandbox App

The release sandbox app that is publicly available for merchants to install doesn't work on emulators for security reasons. An internal debug build of the PayPal app can be used instead to test app switch on an emulator.

1. Go to https://android-jenkins.qa.paypal.com/job/AndroidBuild-CVA-fork-build/.
2. Under the "Last Successful Artifacts" section, download the "consumer-venice-google-dexguardDebug-\<version\>-forkBuild.apk" file. If that file can't be found, download the "consumer-venice-google-debug-\<version\>-forkBuild.apk" file.
3. Install the APK onto your emulator by dragging the APK file from your file explorer onto your emulator
4. This should download the "PayPal" and "PayPal Developer Settings" app. It's a good idea to put these apps onto the home screen of your emulator.

### Configuring the Debug PayPal App

The debug build of the app points to the live endpoint by default. You need to configure it to point to the sandbox endpoint.

1. Open the "PayPal Developer Settings" app
2. Open the "Endpoints" tab
3. In here, open the sandbox endpoint in the list and click the select button ("https://api.sandbox.paypal.com")
4. Kill the "PayPal Developer Settings" app and open the "PayPal" app
5. Log in to a personal sandbox account. You should see a screen saying something about how this is a sandbox app, and there shouldn't be any functionality to the app.

### Enable App Links for the Debug PayPal App and for this Demo App

App links are a way to identify an app using a HTTP or HTTPS scheme (URL) and is used as part of the redirection between apps.

Follow these steps to enable app links on the Debug PayPal app and on the demo app (under section "On both PayPal and Demo app verify PayPal app links"): https://paypal.atlassian.net/wiki/spaces/Checkout/pages/1129122168/Android+Stage+Testing+instructions+for+BT+Demo+app+and+In+App+Checkout+in+Venice#On-both-PayPal-and-Demo-app-verify-PayPal-app-links.

## Development Notes

The below is only relevant if you want to build your own native Android integration of app switch. If you do, here are some notes to be aware of.

### Localhost

Anything that you host on localhost on your host machine (i.e. your laptop, not the emulator) can be accessed on the Android Studio emulator using the IP 10.0.2.2. So, if your server is locally hosted and normally hosted on localhost:8888, you would make API calls from your Android app to the endpoint 10.0.2.2:8888.

### Dependency Management

The /build.gradle.kts file contains project-level information, and the /app/build.gradle.kts contains app-level information. In Android development, there are plugins and dependencies. Plugins are defined in both the /build.gradle.kts file and the /app/build.gradle.kts file. Dependencies are only defined in the /app/build.gradle.kts file.

For an app switch integration (and native Android integration in general), you will likely need a serialization plugin, and HTTP/network request and persistent storage dependencies.

### Allowing Internet

By default, Android apps don't allow themselves to connect to the internet.

This needs to be explicitly allowed in the /app/src/main/AndroidManifest.xml file by nesting a `<uses-permission />` tag within the `<manifest>` tag:

    <?xml version="1.0" encoding="utf-8"?>
    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:tools="http://schemas.android.com/tools">

        <uses-permission android:name="android.permission.INTERNET" />

        <!-- ... -->

    </manifest>

### App Links and Deep Links

The native SDK requires an app link and, for app switch, requires a deep link. These are protocols that can be used to identify your app and is used in app switch to know which app to return to. An app link is defined using an HTTP or HTTPS protocol such that it's a URL that identifies your app. A deep link is a native identifier of your app.

App links and deep links are defined in the /app/src/main/AndroidManifest.xml file under the `<activity>` tag.

An app link is defined as:

    <?xml version="1.0" encoding="utf-8"?>
    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:tools="http://schemas.android.com/tools">

        <application
            <!-- ... -->
            >
            <activity
                <!-- ... -->
                >

                <!-- app link -->
                <intent-filter android:autoVerify="true">
                    <action android:name="android.intent.action.VIEW" />
                    <category android:name="android.intent.category.DEFAULT" />
                    <category android:name="android.intent.category.BROWSABLE" />

                    <data android:scheme="https" />
                    <data android:host="playgroundappswitch.android.com" />
                </intent-filter>

                <!-- ... -->

            </activity>
        </application>

        <!-- ... -->

    </manifest>

Here, the `<data />` tag with the `android:scheme` attribute defines the scheme of the URL (HTTP or HTTPS). Braintree only allows HTTPS app links to be passed. The `<data />` tag with the `android:host` attribute defines the domain of the URL. So, this app link is "https://playgroundappswitch.android.com".

Any app link that you want to use needs to be registered in the Braintree gateway: https://developer.paypal.com/braintree/docs/guides/paypal/checkout-with-paypal/android/v5/#register-app-link-in-control-panel. Only the domain needs to be registered, not the scheme, so you would register "playgroundappswitch.android.com" in this case.

A deep link is defined as:

    <?xml version="1.0" encoding="utf-8"?>
    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:tools="http://schemas.android.com/tools">

        <application
            <!-- ... -->
            >
            <activity
                <!-- ... -->
                >

                <!-- deep link -->
                <intent-filter>
                    <action android:name="android.intent.action.VIEW" />
                    <category android:name="android.intent.category.DEFAULT" />
                    <category android:name="android.intent.category.BROWSABLE" />
                    <data android:scheme="\${applicationId}.braintree" />
                </intent-filter>

                <!-- ... -->

            </activity>
        </application>

        <!-- ... -->

    </manifest>

Here, the `<data />` tag defines the scheme of the deep link. The deep link is required to end with ".braintree" and this is the recommended deep link scheme to use (I haven't tested if this ".braintree" is actually a needed requirement).

The "${applicationId}" is a build configuration variable value that can be found in /app/src/build.gradle.kts. In this case, the "applicationId" is "com.example.braintree", so the deep link would be "com.example.braintree.braintree".

### Allowing Network Traffic

By default, Android apps don't allow network traffic since you are using a user installed certificate. Additionally, they don't allow network traffic to HTTP endpoints due to them being insecure (this can be an issue if your server is locally hosted).

To get around this, you can define a network security configuration that says that your app should allow network traffic through user installed certificates and to specific HTTP routes. This is done by defining a /app/src/main/res/xml/network_security_config.xml file.

The XML file should contain the following configuration as a minimum:

    <?xml version="1.0" encoding="utf-8"?>
    <network-security-config xmlns:tools="http://schemas.android.com/tools">
        <!-- allow network traffic through the user installed CA -->
        <base-config>
            <trust-anchors>
                <!-- Trust preinstalled CAs -->
                <certificates src="system" />
                <!-- Additionally trust user added CAs -->
                <certificates src="user"
                    tools:ignore="AcceptsUserCertificates" />
            </trust-anchors>
        </base-config>

        <!-- allow HTTP network requests to server -->
        <domain-config cleartextTrafficPermitted="true">
            <domain includeSubdomains="true">10.0.2.2</domain>
        </domain-config>
    </network-security-config>

The `<base-config>` tag means that the app should trust network traffic through system installed and user installed certificates.

The `<domain-config>` tag means that the app should allow network traffic to those specific domains. You should replace 10.0.2.2 with whatever your HTTP domain is. To trust multiple domains, simply nest another `<domain>` tag with the same format.

To tell the Android app to actually use your network security configuration you've defined, you need to link to it in the /app/src/main/AndroidManifest.xml file.

Specifically, you need to add the `android:networkSecurityConfig` attribute to your `<application>` tag:

    <?xml version="1.0" encoding="utf-8"?>
    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:tools="http://schemas.android.com/tools">

        <application
            android:networkSecurityConfig="@xml/network_security_config"
            <!-- ... -->
            >
            <!-- ... -->
        </application>

        <!-- ... -->

    </manifest>
