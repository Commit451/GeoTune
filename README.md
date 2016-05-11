#GeoTune

Setup some tunes that play when you enter geofences, aka GeoTunes!

<img src="/assets/screenshot-1.png?raw=true" width="200px">
<img src="/assets/screenshot-2.png?raw=true" width="200px">
<img src="/assets/screenshot-3.png?raw=true" width="200px">

<a href="https://play.google.com/store/apps/details?id=com.jawnnypoo.geotune">
  <img alt="Get it on Google Play"
       src="https://github.com/Commit451/GeoTune/raw/master/assets/google-play-badge-small.png" />
</a>

#Technical
This app provides a nice example of how to set up, register, use and store geofences using the Places API from Google Play Services. It also uses the Places Autocomplete API to allow the user to input any location. GeoTunes are stored using a content provider, which is a bit of overkill, but meh. They are fetched using a Loader (oldskool) and stored using an IntentService.

#Building
If you want the app to build properly, you will need to generate your own API key from the  [Google API Console](https://console.developers.google.com/) and add it to your gradle.properties file (create one in your home directory, under `.gradle`). The app also uses Fabric for Crashlytics, so you might need to generate your own Crashlytics key. All in all, your `gradle.properties` will look something like this:
```Gradle
GEOTUNE_FABRIC_KEY = SomeKeyOnlyReallyNeededForReleaseBuilds
GEOTUNE_GOOGLE_API_KEY = AIzaSySomeApiKeyThatYouHaveGenerated
```

#Where's the History?
This app used to be closed source, and contained a bunch of API keys, so I had to reset the git history. Sorry to all you history buffs

#Contribution
Pull requests are welcomed and encouraged. If you experience any bugs, please [file an issue](https://github.com/Jawnnypoo/open-meh/issues/new)

License
=======

    Copyright 2015 Commit 451

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
