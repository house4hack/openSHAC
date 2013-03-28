// Default empty project template
#include <bb/cascades/Application>
#include <bb/cascades/SceneCover>

#include <QLocale>
#include <QTranslator>
#include "applicationui.hpp"

// include JS Debugger / CS Profiler enabler
// this feature is enabled by default in the debug build only
#include <Qt/qdeclarativedebug.h>
#include "Config.hpp"
#include <bb/platform/RouteMapInvoker>

using namespace bb::cascades;

Q_DECL_EXPORT int main(int argc, char **argv)
{
    qmlRegisterType<shac::config::Configurator>("shac.config", 1, 0, "Configurator");

    qmlRegisterType<bb::platform::RouteMapInvoker>("bb.platform", 1, 0, "RouteMapInvoker");
    // The SceneCover is registered so that it can be used in QML
    qmlRegisterType<SceneCover>("bb.cascades", 1, 0, "SceneCover");

    // Since it is not possible to create an instance of the AbstractCover
    // it is registered as an uncreatable type (necessary for accessing
    // Application.cover).
    qmlRegisterUncreatableType<AbstractCover>("bb.cascades", 1, 0, "AbstractCover",
            "An AbstractCover cannot be created.");

    // this is where the server is started etc
    Application app(argc, argv);

    // localization support
    QTranslator translator;
    QString locale_string = QLocale().name();
    QString filename = QString( "openSHAC_BB_%1" ).arg( locale_string );
    if (translator.load(filename, "app/native/qm")) {
        app.installTranslator( &translator );
    }

    new ApplicationUI(&app);

    // we complete the transaction started in the app constructor and start the client event loop here
    return Application::exec();
    // when loop is exited the Application deletes the scene which deletes all its children (per qt rules for children)
}
