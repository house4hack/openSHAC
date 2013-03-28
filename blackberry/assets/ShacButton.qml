import bb.cascades 1.0

ImageButton {
    property string id
    property string imageName
    property string action
    
    defaultImageSource: "asset:///images/" + imageName + ".png"
    pressedImageSource: "asset:///images/" + imageName + "_pinched.png"
    horizontalAlignment: HorizontalAlignment.Center
    layoutProperties: StackLayoutProperties {
        spaceQuota: 1
    }
    
    onClicked: {
        progress.show()
        
        // clear cookies and reset the cookie header
        webRequester.storage.clearCookies()
        // change url to force reloading with headers if the url is still the same
        webRequester.url = "about:blank"
        webRequester.settings.customHttpHeaders = {
            "Cookie": "token=" + config.accessToken,
            "X-SHAC-Token": config.md5(hwInfo.imei),
            "User-Agent": "openSHAC for BlackBerry 10, " + hwInfo.deviceName + " " + hwInfo.modelName
        }
        // make the request
        webRequester.url = "http://enter.house4hack.co.za/init/android/" + action
    }
}