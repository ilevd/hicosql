
#-- Comments start with # - for YAML and -- for SQL, so it syntax highlighting would work no matter what
#-- extension .yaml or .sql you choose for a file. I prefer .sql.

#-- Some consts which we would use below:
old: 60
young: 20


#-- Let's define our first query:
users: |
  SELECT id, first_name, last_name, age, address_id FROM users

#-- Now we can use it for defining another query. Here we use 'young' which will be expand to 20.
young_users: |
  &users WHERE age < &young

#-- Yet another query
old_users: |
  &users WHERE age > &old

#-- Now we can combine two previous queries:
old_and_young_users: |
  &young_users
  UNION
  &old_users

#-- Each query can use all queries (better say values), defined above
mid_age_users: |
  SELECT * FROM users WHERE id NOT IN (SELECT id FROM ( &old_and_young_users ))

#-- It was the first main feature of HiCoSQL - substitution for &<name>. 
#-- Now let's use the second: templates
#-- Here we define our first template, which use @<name> - the place there the engine inserts
#-- provided value
users_templ: |
  SELECT @fields FROM users

#-- So let's use our template
#-- The <name>:<template_name> is a special short syntax. It means that users1 is based on 'users_templ'.
#-- We provide the map with key-values to substitute. 
#-- The interesting part here, what JSON is a subset of YAML, so we need anything special to parse it
#-- in a library
users1:users_templ: {fields: 'first_name, last_name'}

users2:users_templ: {fields: 'first_name, age'}

#-- So, it was the main two features.
#-- But, because it essentialy a config file, you can write any other data, which you can use later in your 
#-- Clojure code.
#-- Suppose, you want to describe some transactions or tasks:

tasks1: 
  - clear_user_tample
  - insert_users

#-- Such datathings wiil be ignored - it is up to you to make the functionality of your application.
