// Main screen
import bb.cascades 1.0
import bb.system 1.0
import bb.device 1.0
import bb.platform 1.0
import shac.config 1.0

NavigationPane {
    id: navigationPane
    // creates one page with a label
    Page {
        titleBar: TitleBar {
            title: "Smart House Access Control"
        }

        actions: [
            ActionItem {
                title: qsTr("Drive There")
                imageSource: "asset:///images/ic_map.png"
                ActionBar.placement: ActionBarPlacement.OnBar
                onTriggered: {
                    routeInvokerID.go();
                }
            },
            ActionItem {
                id: menuSignIn
                title: qsTr("Sign In")
                imageSource: "asset:///images/ic_login.png"
                onTriggered: root.signIn()
                ActionBar.placement: ActionBarPlacement.InOverflow
            },
            ActionItem {
                id: menuSignOut
                title: qsTr("Sign Out")
                imageSource: "asset:///images/ic_logout.png"
                onTriggered: {
                    root.signOut()
                }
                ActionBar.placement: ActionBarPlacement.InOverflow
            },
            ActionItem {
                title: qsTr("About")
                imageSource: "asset:///images/ic_info.png"
                onTriggered: {
                    aboutDialog.show()
                }
                ActionBar.placement: ActionBarPlacement.InOverflow
            }
        ]

        attachedObjects: [
            ComponentDefinition {
                id: o_auth
                source: "o_auth.qml"
            },
            Configurator {
                id: config
            },
            SystemDialog {
                id: loginDialog
                modality: SystemUiModality.Application
                title: qsTr("Not authenticated")
                body: qsTr("Would you like to sign in to Google for authentication?")
                onFinished: {
                    if (result == SystemUiResult.ConfirmButtonSelection) {
                        root.signIn();
                    }
                }
            },
            SystemDialog {
                id: aboutDialog
                modality: SystemUiModality.Application
                title: qsTr("About SHAC")
                body: qsTr("SHAC allows you to open the door and gate at the House4Hack hackerspace in Centurion, South Africa. SHAC is open source, see http://github.com/house4hack/openSHAC")
            },
            SystemProgressDialog {
                id: progress
                modality: SystemUiModality.Application
                title: qsTr("Connecting")
                progress: -1
                dismissAutomatically: false
            },
            SystemToast {
                id: toast
            },
            OrientationHandler {
                id: orientationHandler
                onOrientationAboutToChange: {
                    root.reOrient(orientation);
                }
            },
            HardwareInfo {
                id: hwInfo
            },
            RouteMapInvoker {
                id: routeInvokerID
                endAddress: "4 Burger ave, Centurion, South Africa"
                endLatitude: -25.839165
                endLongitude: 28.207317
                endName: "House4Hack, Centurion"
                endDescription: "www.house4hack.co.za"
            },
            ComponentDefinition {
                id: appCover
                source: "AppCover.qml"
            }
        ]

        Container {
            id: root
            verticalAlignment: VerticalAlignment.Fill
            horizontalAlignment: HorizontalAlignment.Fill

            layout: StackLayout {
                id: buttonLayout
            }

            function signIn() {
                var page = o_auth.createObject();
                navigationPane.push(page);
            }

			function signOut() {
                config.resetAccessToken()
                updateMenu()
                loginDialog.show()
            }

            function reOrient(orientation) {
                if (orientation == UIOrientation.Landscape) {
                    buttonLayout.orientation = LayoutOrientation.LeftToRight;
                } else {
                    buttonLayout.orientation = LayoutOrientation.TopToBottom;
                }
            }
            
            function updateMenu() {
                if (! config.accessToken) {
                    menuSignIn.enabled = true
                    menuSignOut.enabled = false
                } else {
                    menuSignIn.enabled = false
                    menuSignOut.enabled = true
                }
            }

            ShacButton {
                id: btnGate
                imageName: "gate_small_round"
                action: "gate"
            }

            ShacButton {
                id: btnDoor
                imageName: "door_small_round"
                action: "door"
            }

            WebView {
                id: webRequester
                maxHeight: 200
                visible: false
                verticalAlignment: VerticalAlignment.Bottom
                horizontalAlignment: HorizontalAlignment.Center
                onLoadingChanged: {
                    if (webRequester.url != "about:blank" && loadRequest.status == WebLoadStatus.Succeeded) {
                        webRequester.evaluateJavaScript("navigator.cascades.postMessage(document.body.innerText)")
                    }
                }
                
                onMessageReceived: {
                    progress.cancel()
                    if (message.data.toString() == "Invalid User") {
                        root.signOut()
                    }

                    // done loading, get the JSON
                    toast.body = message.data
                    toast.show()
                }
            }
        }

        onCreationCompleted: {
            Application.cover = appCover.createObject();
            OrientationSupport.supportedDisplayOrientation = SupportedDisplayOrientation.All;
            root.reOrient(orientationHandler.orientation);

            if (! config.accessToken) {
                loginDialog.show()
            }
            
            root.updateMenu()
        }
    }

    onPopTransitionEnded: {
        page.destroy();
    }
}