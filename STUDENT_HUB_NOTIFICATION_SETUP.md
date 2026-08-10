# Student Hub notification update

Android app notifications are kept separate from the web notification UI.

Supported data payload fields:
- type: global | announcement | class | message | mention | reply | tag | force | important
- title: notification title
- body: notification body
- classId: optional class identifier
- mentioned: true/false (used when message mode is `mentions`)

Ordinary notifications respect the Android app settings.
`force` and `important` notifications intentionally bypass ordinary notification toggles.

Settings keys:
- global
- announcements
- app_enabled
- web_app
- message_mode: all | mentions | off
- class_<classId>

The web app should write these settings through the existing WebView bridge if it wants
the web settings screen to control Android notification preferences.
