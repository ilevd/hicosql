# Highly Configurable SQL

A Clojure library designed to manage your big SQL queries based on [YAML](https://yaml.org/).

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

### Why do I need it?

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

#-- It was the first main feature of HiCoSQL - substitution for &<name>. 
#-- Now let's use the second: templates
#-- Here we define our first template, which use @<name> - the place there the engine inserts
#-- provided value
users_templ: |
  SELECT @fields FROM users

#-- So let's use our template
#-- The <name>:<template_name> is a special short syntax. It means that users1 is based on 'users_templ'.
#-- We provide the map with key-values to substitute. 
#-- The interesting part here, that JSON is a subset of YAML, so we need anything special to parse it
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

#-- Such datathings will be ignored - it is up to you to make the functionality of your application.

```

You can put this code to YAML-to-JSON converter [here](https://www.browserling.com/tools/yaml-to-json),
[here](https://www.json2yaml.com/) or [here](https://codebeautify.org/yaml-to-json-xml-csv) and see what it really is.

It will be something like that:

```
{
  "old": 60,
  "young": 20,
  "users": "SELECT id, first_name, last_name, age, address_id FROM users\n",
  "young_users": "&users WHERE age < &young\n",
  "old_users": "&users WHERE age > &old\n",
  "old_and_young_users": "&young_users\nUNION\n&old_users\n",
  "mid_age_users": "SELECT * FROM users WHERE id NOT IN (SELECT id FROM ( &old_and_young_users ))\n",
  "users_templ": "SELECT @fields FROM users\n",
  "users1:users_templ": {
    "fields": "first_name, last_name"
  },
  "users2:users_templ": {
    "fields": "first_name, age"
  },
  "tasks1": [
    "clear_user_tample",
    "insert_users"
  ]
}
```

That's exactly how the library see the file. All it does, is expansion and substitution.

```clojure 
(ns my-new-project.core
  (:require [hicosql.core :as hico]))
  
(hico/run-hico "sql/test.sql")
```

The hico call above will produce such data:

```edn
#ordered/map([:old 60]
             [:young 20]
             [:users "SELECT id, first_name, last_name, age, address_id FROM users\n"]
             [:young_users "SELECT id, first_name, last_name, age, address_id FROM users\n WHERE age < 20\n"]
             [:old_users "SELECT id, first_name, last_name, age, address_id FROM users\n WHERE age > 60\n"]
             [:old_and_young_users
              "SELECT id, first_name, last_name, age, address_id FROM users
                WHERE age < 20
               
               UNION
               SELECT id, first_name, last_name, age, address_id FROM users
                WHERE age > 60
               
               "]
             [:mid_age_users
              "SELECT * FROM users WHERE id NOT IN (SELECT id FROM ( SELECT id, first_name, last_name, age, address_id FROM users
                WHERE age < 20
               
               UNION
               SELECT id, first_name, last_name, age, address_id FROM users
                WHERE age > 60
               
                ))
               "]
             [:users_templ "SELECT @fields FROM users\n"]
             [:users1 "SELECT first_name, last_name FROM users\n"]
             [:users2 "SELECT first_name, age FROM users\n"]
             [:tasks1 ["clear_user_tample" "insert_users"]])
```

'ordered-map' here is just a implementation of (ordered-map)[https://github.com/flatland/ordered].
You can use it as an ordinary hash-map.

## And, that's all?

Yes. That is.

## But... Isn't something missing here? How can I run my queries?

It's easy. Remember, for now it's just something like template engine for SQL queries. So to, run our
queries in more practical way, let's rewrite some queries using `:<name>` notation that will be using
with HugSQL.

```sql

young: 20 

users: |
  SELECT first_name, last_name FROM users 
  WHERE age > &young AND salary > :salary
```

Here, in our syntethic example, `young` is a constant param that rarely should be changed,
and `salary` is what we suppose to be changeable within requests to a database.


Clojure code:


```clojure 
(ns my-new-project.core
  (:require [hicosql.core :as hico]
            [hugsql.core :as hug]
            [clojure.jdbc :as jdbc]))
  
;; You should probably use some stage management system here, but for now just 'def'
(def queries (hico/run-hico "sql/test.sql")


;; hugsql/sqlvec-fn is a function what returns function from SQL string,
;; that creates sqlvec from the passed params
(defn make-request [db-spec query-key params]
  (jdbc/query 
    db-spec 
    ((hugsql/sqlvec-fn (-> queries query-key)) params))))

    
;; Now we're ready to make some requests
(make-request your-db-spec :users {:salary 100000}))  

```

It's just a simple example of usage. In production you'd rather want to write macros to create your
requests functions in compile-time. Or at least cache the call to `hugsql/sqlvec-fn`.


The library is in an experimental stage, the help is appreciated.


## License
Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

ilevd © 2019
