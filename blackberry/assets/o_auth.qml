// Default empty project template
import bb.cascades 1.0
import bb.system 1.0
import shac.config 1.0

// OAUth URL: https://accounts.google.com/o/oauth2/auth?scope=https://www.googleapis.com/auth/userinfo.email&response_type=code&redirect_uri=http://localhost:8080&client_id=231571905235.apps.googleusercontent.com

// creates one page with a label
Page {
    id: page
    
    Container {
        id: root
        
        layout: StackLayout {
        }

        attachedObjects: [
            SystemToast {
                id: toast
            },
            Configurator {
                id: config
            }
        ]
            
        ProgressIndicator {
            id: progress
            fromValue: 0
            toValue: 100
            value: 0
            horizontalAlignment: HorizontalAlignment.Fill
        }

        ScrollView {
            WebView {
                id: webView
                property string token
                
                url: "https://accounts.google.com/o/oauth2/auth?scope=https://www.googleapis.com/auth/userinfo.email&response_type=code&redirect_uri=http://localhost:8080&client_id=231571905235.apps.googleusercontent.com&access_type=offline"
                onNavigationRequested: {
                    console.log("Navigation Requested: " + request.url + " navigationType=" + request.navigationType)
                    var url = request.url.toString()
                    if (url.indexOf("http://localhost:8080/") == 0) {
                        token = url.substring("http://localhost:8080/?code=".length)
                        console.log("Auth token: " + token)
                        var postHtml = "<html><body>Authenticating...<form action='https://accounts.google.com/o/oauth2/token' method='POST'>" + "<input type='hidden' name='code' value='{code}'/>" + "<input type='hidden' name='client_id' value='231571905235.apps.googleusercontent.com'/>" + "<input type='hidden' name='client_secret' value='_TQPhcCcw7eAra4PUaY74m_W'/>" + "<input type='hidden' name='grant_type' value='authorization_code'/> " + "<input type='hidden' name='redirect_uri' value='http://localhost:8080'/>" + "</form><script type='text/javascript'>document.forms[0].submit();</script></body></html>"
                        postHtml = postHtml.replace("{code}", token)
                        webView.loadHtml(postHtml,"")
                    }
                }
                onLoadingChanged: {
                    if (loadRequest.status == WebLoadStatus.Succeeded) {
                        progress.visible = false
                        if (token != null) {
                            // read the resulting json using javascript
                            webView.evaluateJavaScript("navigator.cascades.postMessage(document.body.innerText)")
                        }
                    } else {
                        progress.visible = true
                    }
                }
                onLoadProgressChanged: {
                    progress.value = loadProgress
                }
                onMessageReceived: {
                    eval("var json = " + message.data)
                    if (json.error) {
                        toast.body = json.error
                        toast.show()
                        page.parent.pop()
                    } else {
                        config.accessToken = json.access_token
                        webView.html = qsTr("Successfully authenticated!")
                        toast.body = qsTr("Successfully authenticated!")
                        toast.show()
                        page.parent.pop()
                    }
                }
            }
        }
    }
}
