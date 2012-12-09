# a table to store latest rolling code
db.define_table('rolling_code_seeds',
    Field('myindex','integer',length=8),
    Field('m_w','integer',length=32, default=20110715),
    Field('m_z','integer',length=32, default=20110911))

# a table to store users and their roles
db.define_table('valid_members',
    Field('emailaddr',requires=IS_NOT_EMPTY()),
    Field('user','boolean',default=1),
    Field('admin','boolean',default=0))

# a table to store remote arduino shac url
db.define_table('shac_config',
    Field('myindex','integer',length=8),
    Field('shac_url',requires=IS_NOT_EMPTY()))

# ValidMembers.insert(emailaddr='philipbooysen@gmail.com',user=1,admin=1)

# and define some global variables that will make code more compact
RollingCodeSeeds = db.rolling_code_seeds
ValidMembers = db.valid_members
ShacConfig = db.shac_config
uid = auth.user_id

