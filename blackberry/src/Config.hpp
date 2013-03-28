#ifndef CONFIG_HPP
#define CONFIG_HPP

#include <QtCore/QtCore>
#include <QString>

using namespace std;

namespace shac
{
    namespace config
    {
        class Configurator : public QObject
        {
            Q_OBJECT

            public:
                Configurator();
                ~Configurator();

                Q_PROPERTY(QString accessToken READ getAccessToken WRITE setAccessToken RESET resetAccessToken NOTIFY accessTokenChanged)
                Q_PROPERTY(QString server READ getServer WRITE setServer RESET resetServer NOTIFY serverChanged)
                Q_PROPERTY(QString port READ getPort WRITE setPort RESET resetPort NOTIFY portChanged)

                Q_INVOKABLE void read();
                Q_INVOKABLE void write();

                Q_INVOKABLE QString getAccessToken();
                Q_INVOKABLE void setAccessToken(QString);
                Q_INVOKABLE void resetAccessToken();

                Q_INVOKABLE QString getServer();
                Q_INVOKABLE void setServer(QString);
                Q_INVOKABLE void resetServer();

                Q_INVOKABLE QString getPort();
                Q_INVOKABLE void setPort(QString);
                Q_INVOKABLE void resetPort();

                Q_INVOKABLE QString md5(QString value);

            signals:
                void accessTokenChanged();
                void serverChanged();
                void portChanged();

            private:
                QString accessToken;
                QString server;
                QString port;
                QSettings settings;
        };
    }
}

#endif
