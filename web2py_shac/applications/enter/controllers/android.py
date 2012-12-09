def requires_user():
    if(auth.user !=None):
        if db((ValidMembers.emailaddr==auth.user.email) & (ValidMembers.user==True)).select():
            return 1
        else:
            return 0
    else:
        return 0

def tofile(url):
    f = open('/home/schalk/tmp/web2py.txt','a')
    f.write(url+'\n')
    f.close()

def openIt(url):
    if requires_user():
        from gluon.tools import fetch
        try:
            #mypage = fetch("http://192.168.1.80/"+url)
            tofile(url)
            res = T('Success')
        except IOError:
            res = T('Arduino SHAC or link to it down?')
        return res
    else:
        return T("Invalid User")


def gate():
    return openIt("gate")

def door():
    return openIt("door")


