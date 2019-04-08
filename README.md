# Highly Configurable SQL

A Clojure library designed to manage you big SQL queries based on [YAML](https://yaml.org/).

## Installation

### Leiningen

[![Clojars Project](https://img.shields.io/clojars/v/hicosql.svg)](https://clojars.org/hicosql)

## Description
1. Easy to read and write
2. Highly configurable
3. Composable queries
4. Library itself is pretty simple 
5. Designed to work with big complex SQL queries
6. SQL as configs

### Why do we need it?

There are a lot of different SQL libraries. HugSQL, HoneySQL, YeSQL...

But unfourtanetly they provide good query composability only on Clojure level.

Then SQL code base is large, and you want to provide great readability, for example, for DBA's who
don't know Clojure you better need something like template language for your SQL than Clojure-like
solutions.  

So, the idea of this library is to manage SQL like configurations.

Each configuration file for HiCoSQL is a YAML file with keys and values that library expand.

So let's get started.

Suppose we have the next file:


```sql

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

#-- It was the first main feature on HiCoSQL - substitution for &<name>. 
#-- Now let's use the second: templates
#-- Here we define our first template, which use @<name> - the place there the engine inserts
#-- provided value
users_templ: |
  SELECT @fields FROM users

#-- So let's use our template
#-- The <name>:<template_name> is a special short syntax. It means that users1 is based on 'users_templ'
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

#-- Such data things wiil be ignored - it is up to you make the functionality of your application.

```




## License
Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

ilevd © 2019
