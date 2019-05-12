#-- Comments start with # - for YAML and -- for SQL, so it syntax highlighting would work no matter what
#-- extension .yaml or .sql you choose for a file. I prefer .sql.

#-- First of all, there are special engines directives, started with '__'
#-- This one includes the content of a file in current file.
#-- This is useful then you have some constants or queries that you use in different files
__include: 'base/common.sql'


#-- Some constants which we would use below:
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

#-- Each query can use all queries defined above
mid_age_users: |
  SELECT * FROM users WHERE id NOT IN (SELECT id FROM ( &old_and_young_users ))

#-- In the real life we need to provide some values from Clojure code, for that we use :<name> strings
#-- which will be replaced with provided values. Let's define some queries.
query1: |
  SELECT * FROM (&users)
  WHERE age = :age AND salary = :salary AND address = :addr AND project = :project

query2: |
  SELECT max(salary) FROM users WHERE age = :age AND num = :num

#-- We can replace some of these parameters in next queries.
#-- Here we use query1 and query2 as functions calls with supplied parameters.
#-- The sign '!' on the end of query name means that we pass parameters in form:
#-- :key1 value1 :key2 value2 ... etc.
#-- We don't pass :project value because we suppose to use it from Clojure code.
query3: |
  SELECT * FROM &(query1! :age 20
                          :salary (query2! :age 40)
                          :num (* old young 30)
                          :addr "'Some address, Street 1'")
  UNION ALL
  SELECT * FROM some_other_table WHERE a_lot_of_conditions

#-- We can also use queries calls without additional keys before arguments,
#-- but for that we need to define query in function form to describe arguments order.
#-- Suppose we have:
q1(a, b, c, d): |
  SELECT :d, :c, :b, :a FROM some_table

#-- Now we can use it without additional keys. And in that case we don't use '!' at the end. Just 'q1'
q2: |
  SELECT some_sql FROM &(q1 "SQL expression" "'string with quotes'" 10 young)

#-- But of course, we can also use it in full form:
q3: |
  SELECT some_sql FROM &(q1! :a "SQL expression" :b "'string with quotes'" :c 10 :d young)
