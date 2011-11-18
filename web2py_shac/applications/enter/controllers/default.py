
# a public home page for our service
def index():
	if auth.user: redirect(URL('home'))
	return locals()

# login, registration, etceterm
def user():
	return dict(form=auth())

# to download images
def download():
	return response.download(request,db)

# for services
def call():
	session.forget()
	return service()

# our landing page
@auth.requires_login()
def home():
	#ValidMembers.truncate()
	#ShacConfig.truncate()
	response.menu.append((T('Admin'), False, URL('init','default','admin')))
	return locals()

## Define a function to assess users' admin access
@auth.requires_login()
def requires_admin():
	if db((ValidMembers.emailaddr==auth.user.email) & (ValidMembers.admin==True)).select():
		return 1
	else:
		session.flash=T('Not logged in as administrator')
		redirect(URL('home'))
		return 0

## Define a function to assess users' access
@auth.requires_login()
def requires_user():
	if db((ValidMembers.emailaddr==auth.user.email) & (ValidMembers.user==True)).select():
		return 1
	else:
		session.flash=T('Not logged in as valid user')
		redirect(URL('home'))
		return 0

@auth.requires_login()
def admin():
	requires_admin()
	response.menu = []
	#response.menu.append((T('Admin'), False, URL('init','default','admin'), [(T('Admin SHAC'), False, URL('init','default','admin_shac'))]))
	#response.menu.append((T('Admin SHAC'), False, URL('init','default','admin_shac'),),(T('Admin Users'), False, URL('init','default','admin_users'),))
	response.menu = [
        (T('Admin SHAC'), False, URL('init','default','admin_shac')),
        (T('Admin Users'), False, URL('init','default','admin_users')),
	]
	#response.menu.append((T('Admin'), False, URL('init','default','admin')))
	return locals()

# our admin_shac page:
#  - be able to reset/resync arduino rolling code sequence
#  - set the remote address and port of arduino shac
@auth.requires_login()
def admin_shac():
	requires_admin()
	response.menu = []
	form=FORM()
	if form.accepts(request.vars,formname='resync_arduino'):
		if request.vars.led1 is None:
			request.vars.led1="0"
		else:
			request.vars.led1="1"
		if request.vars.led2 is None:
			request.vars.led2="0"
		else:
			request.vars.led2="1"
		if request.vars.led3 is None:
			request.vars.led3="0"
		else:
			request.vars.led3="1"
		if request.vars.led4 is None:
			request.vars.led4="0"
		else:
			request.vars.led4="1"
		if request.vars.led5 is None:
			request.vars.led5="0"
		else:
			request.vars.led5="1"
		led_selection=request.vars.led1 + request.vars.led2 + request.vars.led3 + request.vars.led4 + request.vars.led5
                shacconfigs = db(ShacConfig.myindex==1).select()
       		for config in shacconfigs:
			shac_url=config.shac_url
		import re
		import random
		cloud_seed = random.randrange(1,2147483647,1)
		from gluon.tools import fetch
		try:
			mypage = fetch(shac_url + "/resync?" + str(cloud_seed) + "&" + led_selection)
		except IOError:
			response.flash=T('Arduino SHAC or link to it down?!')
		else:
			p = re.compile('[a-z]+')
			if re.match( r'(.*)Resync Success(.*)', mypage, ):
				arduino_seed=re.sub( r'(.*)Resync Success:(\d+)(.*)', r'\2', mypage)
				db(RollingCodeSeeds.myindex==1).update(m_w=cloud_seed)
     		  		db(RollingCodeSeeds.myindex==1).update(m_z=int(arduino_seed))
				response.flash=T('Arduino SHAC now synchronised!')
			# else it failed
			else:
				response.flash=T('Arduino SHAC synchronisation failed!')
	elif form.accepts(request.vars,formname='set_seed'):
		#request.vars.seed1
		#request.vars.seed2
		seeds = db(RollingCodeSeeds.myindex==1).select()
		if len(seeds) == 0:
			RollingCodeSeeds.insert(myindex=1,m_w=request.vars.seed1,m_z=request.vars.seed2)
        	else:
			db(RollingCodeSeeds.myindex==1).update(m_w=request.vars.seed1)
        		db(RollingCodeSeeds.myindex==1).update(m_z=request.vars.seed2)
		response.flash=T('Cloud H4H SHAC rolling code now re-seeded!')
	elif form.accepts(request.vars,formname='shac_url'):
		shacconfigs = db(ShacConfig.myindex==1).select()
		if len(shacconfigs) == 0:
			ShacConfig.insert(myindex=1,shac_url=request.vars.gateurl)
        	else:
			db(ShacConfig.myindex==1).update(shac_url=request.vars.gateurl)
		response.flash=T('Remote Arduino SHAC Url config saved!')
	else:
	  	response.flash=T('Select an administrative function')
	shacconfigs = db(ShacConfig.myindex == 1).select()
	#shacconfigs = db(ShacConfig).select()
	#countme=db(ShacConfig).count()
	return locals()

# our admin_users page:
#  - add valid gate users
#  - make some gate users admin users
#  - watch audit trail of access and actions
@auth.requires_login()
def admin_users():
	requires_admin()
	response.menu = []
	response.flash=T('Select an administrative function')
	form1 = SQLFORM(ValidMembers, fields=['emailaddr','admin'],_formname='adduser')
	if form1.accepts(request.vars, formname='adduser'):
		response.flash = 'User added'
	elif form1.errors:
		response.flash = 'User addition had errors'
	ValidUsers = db(ValidMembers).select()
	#record = ValidMembers(request.args(0))
	if (request.args(0)):
		record = ValidMembers(request.args(0))# or redirect(URL('init','default','admin_users'))
		form = SQLFORM(ValidMembers, record, headers={'ValidMembers.emailaddr': 'Email'}, fields=['emailaddr','user','admin'], deletable=True)
		if form.accepts(request.vars, session):
			session.flash = 'form accepted'
			form = ""
			#redirect(URL('admin_users'))
			redirect(URL('init','default','admin_users',args=''))
		elif form.errors:
			response.flash = 'form has errors'
	else:
		form = ""

	# make the form
	#crud.settings.formstyle = 'table2cols'
	#form = crud.create(ValidMembers)
#	record=db(ValidMembers.myindex == 1)
#	form_users = SQLFORM(db.ValidMembers, record)
#	record = db(ValidMembers.myindex == 1)
#	form_users = SQLFORM(ValidMembers,record)
#	if form_users.accepts(request.vars, session):
#		response.flash = 'Users updated'
#	elif form_users.errors:
#		response.flash = 'User changes had errors'
#	else:
#		response.flash = 'Please fill out the form'
#	for view use : {{=form_users}}
	return locals()
	#return dict(form=form)

##http://.../[app]/[controller]/data/update/[tablename]/[id]
#@auth.requires_login()
#def update_ValidMembers():
#	requires_admin()
#	response.menu = []
#	return dict(form=crud.update(ValidMembers, request.args(0)))
#
#@auth.requires_login()
#def tryme():
#	requires_admin()
#	response.menu = []
#	manage(ValidMembers)
#	return local()
#
## Here is another very generic controller function that lets
## you search, create and edit any records from any table where
## the tablename is passed request.args(0): 
#def manage():
#	table=db[request.args(0)]
#	form = crud.update(table,request.args(1))
#	table.id.represent = lambda id: \
#	   A('edit:',id,_href=URL(args=(request.args(0),id)))
#	search, rows = crud.search(table)
#	return dict(form=form,search=search,rows=rows)

# our gate , will show LED sequence and Gate image to be click for attempted access
@auth.requires_login()
def gate():
	#if not requires_user():
	#	redirect(URL('home'))
	requires_user()
	response.menu = []
	form=FORM()
	led_selection=""
	if form.accepts(request.vars,formname='gate'):
		if request.vars.led1 is None:
			request.vars.led1="0"
		else:
			request.vars.led1="1"
		if request.vars.led2 is None:
			request.vars.led2="0"
		else:
			request.vars.led2="1"
		if request.vars.led3 is None:
			request.vars.led3="0"
		else:
			request.vars.led3="1"
		if request.vars.led4 is None:
			request.vars.led4="0"
		else:
			request.vars.led4="1"
		if request.vars.led5 is None:
			request.vars.led5="0"
		else:
			request.vars.led5="1"
		led_selection=request.vars.led1 + request.vars.led2 + request.vars.led3 + request.vars.led4 + request.vars.led5
		#response.flash = 'form accepted'
                shacconfigs = db(ShacConfig.myindex==1).select()
                if len(shacconfigs) == 0:
			ShacConfig.insert(myindex=1,shac_url='http://192.168.1.80')
			shac_url='http://192.168.1.80'
		else:
        		for config in shacconfigs:
				shac_url=config.shac_url
		import re
		from gluon.tools import fetch
		role_code = str(rolling_code())
		try:
			#mypage = fetch("http://127.0.0.1/gate?" + role_code + "&" + led_selection)
			#mypage = fetch("http://192.168.1.80/gate?" + role_code + "&" + led_selection)
			#mypage = fetch(shac_url + "/gate?" + role_code + "&" + led_selection)
			f = open("/home/schalk/tmp/web2py.txt","a")
			f.write("open\n")
			f.close()

			mypage = fetch(shac_url + "/gate")
		except IOError:
			response.flash=T('Arduino SHAC or link to it down?! Code: ' + role_code + '&' + led_selection)
		else:
			p = re.compile('[a-z]+')
			if re.match( r'(.*)Welcome the gate(\.*)', mypage, ):
				response.flash=T('Gate access granted! Code: ' + role_code + '&' + led_selection)
			# else it failed
			else:
				response.flash=T('Gate access denied! Code: ' + role_code + '&' + led_selection)
	else:
		response.flash=T('Select LED sequence and click the gate to open')
	return dict(form=form, ledsequence=led_selection)

# our door , will show LED sequence and Gate/Door image to be click for attempted access
@auth.requires_login()
def door():
	#if not requires_user():
	#	redirect(URL('home'))
	requires_user()
	response.menu = []
	form=FORM()
	led_selection=""
	if form.accepts(request.vars,formname='gate'):
		if request.vars.led1 is None:
			request.vars.led1="0"
		else:
			request.vars.led1="1"
		if request.vars.led2 is None:
			request.vars.led2="0"
		else:
			request.vars.led2="1"
		if request.vars.led3 is None:
			request.vars.led3="0"
		else:
			request.vars.led3="1"
		if request.vars.led4 is None:
			request.vars.led4="0"
		else:
			request.vars.led4="1"
		if request.vars.led5 is None:
			request.vars.led5="0"
		else:
			request.vars.led5="1"
		led_selection=request.vars.led1 + request.vars.led2 + request.vars.led3 + request.vars.led4 + request.vars.led5
		#response.flash = 'form accepted'
                shacconfigs = db(ShacConfig.myindex==1).select()
                if len(shacconfigs) == 0:
			ShacConfig.insert(myindex=1,shac_url='http://192.168.1.80')
			shac_url='http://192.168.1.80'
		else:
        		for config in shacconfigs:
				shac_url=config.shac_url
		import re
		from gluon.tools import fetch
		role_code = str(rolling_code())
		try:
			#mypage = fetch("http://127.0.0.1/gate?" + role_code + "&" + led_selection)
			#mypage = fetch("http://192.168.1.80/gate?" + role_code + "&" + led_selection)
			#mypage = fetch(shac_url + "/gate?" + role_code + "&" + led_selection)
			mypage = fetch(shac_url + "/door")
		except IOError:
			response.flash=T('Arduino SHAC or link to it down?! Code: ' + role_code + '&' + led_selection)
		else:
			p = re.compile('[a-z]+')
			if re.match( r'(.*)Welcome the gate(\.*)', mypage, ):
				response.flash=T('Gate access granted! Code: ' + role_code + '&' + led_selection)
			# else it failed
			else:
				response.flash=T('Gate access denied! Code: ' + role_code + '&' + led_selection)
	else:
		response.flash=T('Select LED sequence and click the gate to open')
	return dict(form=form, ledsequence=led_selection)

@auth.requires_login()
def rolling_code():
	requires_user()
	#RollingCodeSeeds.truncate()
	seeds = db(RollingCodeSeeds.myindex==1).select()
	if len(seeds) == 0:
		RollingCodeSeeds.insert(myindex=1,m_w=20110715)
		RollingCodeSeeds.insert(myindex=1,m_z=20110911)
		m_w = 20110715 & 0xFFFFFFFF
		m_z = 20110911 & 0xFFFFFFFF
	for seed in seeds:
		m_w = seed.m_w & 0xFFFFFFFF
		m_z = seed.m_z & 0xFFFFFFFF
        m_z = (36969 & 0xFFFFFFFF) * (m_z & (65535 & 0xFFFFFFFF)) + (m_z >> 16)
        m_w = (18000 & 0xFFFFFFFF) * (m_w & (65535 & 0xFFFFFFFF)) + (m_w >> 16)
        next_rolling_code = ((m_z << 16) + m_w) & 0xFFFFFFFF
        db(RollingCodeSeeds.myindex==1).update(m_w=m_w)
        db(RollingCodeSeeds.myindex==1).update(m_z=m_z)
	return next_rolling_code

# a page for searching friends and requesting friendship
@auth.requires_login()
def about():
	response.menu = []
	if db(ValidMembers.emailaddr).count()==0:
		ValidMembers.insert(emailaddr='philipbooysen@gmail.com',user=True,admin=True)
	user = User(a0 or me)
	return locals()

