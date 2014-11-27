Ambience
========

<h6>The Simplest Audio Player Library for Android</h6>

<h3>Introduction</h3>
<p>Ambience is a small and powerful android library that helps you build advanced audio apps in minutes. 
Built upon many of android's system components and powerful open source libraries, Ambience is fast, efficient and easy to use.</p>
<p>Ambience is compatible with android mobile, tv and auto. Pass Ambience a playlist filled with tracks then lean back and relax.
Ambience will continue to play audio even when the app is placed in the background.</p>

<pre>Ambience.turnOn(context)
        .setPlaylistTo(playlist)
        .play();</pre>

<br />
<h3>Features</h3>

<h5>Playback Controls</h5>
<p>Customize and play any playlist with just one line of code. Play, Skip, Forward, Rewind, Pause and Volume Control are just a few of the playback controls that Ambience provides.</p>

<pre>Ambience.activeInstance()
        .shuffleAndSetPlaylistTo(playlist)
        .setVolumeTo(0.5f)
        .playFromPosition(3);</pre>


<h5>Custom Notifications</h5>

<p>Ambience creates a custom notification for each track in the playlist. Users can use these notifications to control playback options and jump right back into your app. Notifications created by Ambience will also show up on wearable devices that are connected to the app. For android tv and auto apps, a notification will show as a now playing card in the recommendation section.</p>
<p>To launch an activity from a notification, pass the package and activity name to the LaunchActivity method.</p>

<pre>Ambience.activeInstance()
.setNotificationLaunchActivity(package,activity);</pre>

<h5>Callback Methods</h5>
<p>Get notified when an event occurs or playback options change via the AmbientListener interface. The AmbientListener provides several callback methods that are triggered for the current playing track or when an event occurs in the service.</p>

<pre>Ambience.activeInstance() .listenForUpdatesWith(AmbientListener);</pre>

<h5>AmbientTrack</h5>
<p>Store track meta data with the AmbientTrack class. The AmbientTrack class extends on the android's Parcelable class. This allows an AmbientTrack to be easily integrated or shared between projects.</p>

<pre>public void bindView(View view, Parcelable object) {
    AmbientTrack track = (AmbientTrack) object;
   ((TextView) view).setText(track.getName());
}</pre>

<br />

<h2>How it Works</h2>
<p><strong>Ambience</strong> - A BroadcastReceiver that sends and receivers request from the AmbientService. This class provides methods for controlling audio playback. Ambience will trigger update methods on a callback object that has implemented the AmbientListener interface.</p>

<p><strong>AmbientService</strong> - An Android Service that allows audio playback in the background. The AmbientService listens for a request, performs the request on the current playlist and alerts the Ambience class when done.</p>

<p><strong>AmbientTrack</strong> - A class that holds meta data for a single track.</p>

<p><strong>AmbientListener</strong> - A callback interface that is triggered when an event has occurred in the AmbientService.</p>

<br />

<h3>Version</h3>
<p>Current Version == v1.3</p>
<br />

<h3>Sample Code</h3>
<p>Get the sample code <a href="https://github.com/tonyostudios/AmbienceSampleCode">here</a>.</p>
<br />
<h2>Contribute</h2>
<p>Before submitting a request, please make sure that your code runs on an android device.</p>

<br />
<h2>License</h2>
Copyright 2014 TonyoStudios.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
<br />
<h2>Disclaimer</h2>
<p>The Ambience Library uses Picasso a powerful image downloading and caching library for android. Picasso is property of Square Inc and is provided under the Apache 2.0 License. Visit <a href="http://square.github.io/picasso/" target="_blank">square.github.io/Picasso</a> for more information.</p>
