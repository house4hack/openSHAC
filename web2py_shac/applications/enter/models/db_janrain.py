# we import the module
import os
from gluon.contrib.login_methods.rpx_account import RPXAccount
# We disable actions that will be provided by Janrain, not auth
auth.settings.actions_disabled=['register','change_password','request_reset_password']
# we read the key from the file because we want to keep it private
api_key = open(os.path.join(request.folder,'private','janrain_api_key.txt'),'r').read().strip()
# we connect auth to Janrain
#auth.settings.login_form = RPXAccount(request, api_key=api_key,domain='enter-house4hack', url = "http://localhost:8000/%s/default/user/login" % request.application)
#auth.settings.login_form = RPXAccount(request, api_key=api_key,domain='enter-house4hack', url = "http://localhost:8080/%s/default/user/login" % request.application)
#auth.settings.login_form = RPXAccount(request, api_key=api_key,domain='enter-house4hack', url = "http://192.168.43.16:8000/%s/default/user/login" % request.application)
#auth.settings.login_form = RPXAccount(request, api_key=api_key,domain='enter-house4hack', url = "http://192.168.1.5/%s/default/user/login" % request.application)
#auth.settings.login_form = RPXAccount(request, api_key=api_key,domain='enter-house4hack', url = "http://arduinoh4h.appspot.com/%s/default/user/login" % request.application)
#auth.settings.login_form = RPXAccount(request, api_key=api_key,domain='enter-house4hack', url = "http://enter.house4hack.co.za/%s/default/user/login" % request.application)
auth.settings.login_form = RPXAccount(request, api_key=api_key,domain='enter-house4hack-android', url = "http://fat-sparrow.dyndns-home.com:8000/%s/default/user/login" % request.application)
