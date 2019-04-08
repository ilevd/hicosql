old: 60
young: 20

users: |
  SELECT id, first_name, last_name, age, address_id FROM users

young_users: |
  &users WHERE age < &young

old_users: |
  &users WHERE age > &old

old_and_young_users: |
  &young_users
  UNION
  &old_users

mid_age_users: |
  SELECT * FROM users WHERE id NOT IN (SELECT id FROM ( &old_and_young_users ))


#-- Other example
users_templ: |
  SELECT @fields FROM users

users1:users_templ: {fields: 'first_name, last_name'}

users2:users_templ: {fields: 'first_name, age'}

