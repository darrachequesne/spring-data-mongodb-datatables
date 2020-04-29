[![Build Status](https://api.travis-ci.org/darrachequesne/spring-data-mongodb-datatables.svg?branch=master)](https://travis-ci.org/darrachequesne/spring-data-mongodb-datatables)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.darrachequesne/spring-data-mongodb-datatables/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.darrachequesne/spring-data-mongodb-datatables)

# spring-data-mongodb-datatables

This project is an extension of the [Spring Data MongoDB](https://github.com/spring-projects/spring-data-mongodb) project to ease its use with jQuery plugin [DataTables](http://datatables.net/) with **server-side processing enabled**.

This will allow you to handle the Ajax requests sent by DataTables for each draw of the information on the page (i.e. when paging, ordering, searching, etc.) from Spring **@RestController**.

For its JPA counterpart, please see [spring-data-jpa-datatables](https://github.com/darrachequesne/spring-data-jpa-datatables).

**Example:**

```java
@RestController
public class UserRestController {

  @Autowired
  private UserRepository userRepository;

  @RequestMapping(value = "/data/users", method = RequestMethod.GET)
  public DataTablesOutput<User> getUsers(@Valid DataTablesInput input) {
    return userRepository.findAll(input);
  }
}
```

![Example](https://user-images.githubusercontent.com/13031701/43364754-92f8de16-9320-11e8-9ee2-cc072e1eef8c.gif)


## Contents

- [Maven dependency](#maven-dependency)
- [Getting started](#getting-started)
  - [1. Enable the use of `DataTablesRepository` factory](#1-enable-the-use-of-datatablesrepository-factory)
  - [2. Create a new entity](#2-create-a-new-entity)
  - [3. Extend the DataTablesRepository interface](3-extend-the-datatablesrepository-interface)
  - [4. On the client-side, create a new DataTable object](#4-on-the-client-side-create-a-new-datatable-object)
  - [5. Fix the serialization / deserialization of the query parameters](#5-fix-the-serialization--deserialization-of-the-query-parameters)
- [API](#api)
- [How to](#how-to)
  - [Apply filters](#apply-filters)
  - [Manage non-searchable fields](#manage-non-searchable-fields)
  - [Limit the exposed attributes of the entities](#limit-the-exposed-attributes-of-the-entities)
- [Troubleshooting](#troubleshooting)

## Maven dependency

```xml
<dependency>
  <groupId>com.github.darrachequesne</groupId>
  <artifactId>spring-data-mongodb-datatables</artifactId>
  <version>1.0.3</version>
</dependency>
```

Back to [top](#spring-data-mongodb-datatables).


## Getting started

Please see the [sample project](https://github.com/darrachequesne/spring-data-jpa-datatables-sample) for a complete example.

### 1. Enable the use of `DataTablesRepository` factory

With either

```java
@Configuration
@EnableMongoRepositories(repositoryFactoryBeanClass = DataTablesRepositoryFactoryBean.class)
public class DataTablesConfiguration {}
```

or its XML counterpart

```xml
<mongo:repositories factory-class="org.springframework.data.mongodb.datatables.DataTablesRepositoryFactoryBean" />
```

You can restrict the scope of the factory with `@EnableMongoRepositories(repositoryFactoryBeanClass = DataTablesRepositoryFactoryBean.class, basePackages = "my.package.for.datatables.repositories")`. In that case, only the repositories in the given package will be instantiated as `DataTablesRepositoryImpl` on run.

```java
@Configuration
@EnableMongoRepositories(basePackages = "my.default.package")
public class DefaultConfiguration {}

@Configuration
@EnableMongoRepositories(repositoryFactoryBeanClass = DataTablesRepositoryFactoryBean.class, basePackages = "my.package.for.datatables.repositories")
public class DataTablesConfiguration {}
```

### 2. Create a new entity

```java
@Entity
public class User {

  private Integer id;

  private String mail;

  private Address address;

}
```

### 3. Extend the DataTablesRepository interface

```java
public interface UserRepository extends DataTablesRepository<User, Integer> {}
```

### 4. On the client-side, create a new DataTable object

```javascript
$(document).ready(function() {
  var table = $('table#sample').DataTable({
    'ajax' : '/data/users',
    'serverSide' : true,
    columns : [{
      data : 'id'
    }, {
      data : 'mail'
    }, {
      data : 'address.town',
      render: function (data, type, row) {
        return data || '';
      }
    }]
  });
}
```

### 5. Fix the serialization / deserialization of the query parameters

By default, the [parameters](https://datatables.net/manual/server-side#Sent-parameters) sent by the plugin cannot be deserialized by Spring MVC and will throw the following exception: `InvalidPropertyException: Invalid property 'columns[0][data]' of bean class [org.springframework.data.jpa.datatables.mapping.DataTablesInput]`.

There are multiple solutions to this issue:

- include the [jquery.spring-friendly.js](jquery.spring-friendly.js) file found at the root of the repository

It overrides jQuery data serialization to allow Spring MVC to correctly map input parameters (by changing `column[0][data]` into `column[0].data` in request payload)

- retrieve data with POST requests

Client-side:

```javascript
$('table#sample').DataTable({
  'ajax': {
    'contentType': 'application/json',
    'url': '/data/users',
    'type': 'POST',
    'data': function(d) {
      return JSON.stringify(d);
    }
  }
})
```

Server-side:

```java
@RequestMapping(value = "/data/users", method = RequestMethod.POST)
public DataTablesOutput<User> getUsers(@Valid @RequestBody DataTablesInput input) {
  return userRepository.findAll(input);
}
```

- manually serialize the query parameters

```javascript

function flatten(params) {
  params.columns.forEach(function (column, index) {
    params['columns[' + index + '].data'] = column.data;
    params['columns[' + index + '].name'] = column.name;
    params['columns[' + index + '].searchable'] = column.searchable;
    params['columns[' + index + '].orderable'] = column.orderable;
    params['columns[' + index + '].search.regex'] = column.search.regex;
    params['columns[' + index + '].search.value'] = column.search.value;
  });
  delete params.columns;

  params.order.forEach(function (order, index) {
    params['order[' + index + '].column'] = order.column;
    params['order[' + index + '].dir'] = order.dir;
  });
  delete params.order;

  params['search.regex'] = params.search.regex;
  params['search.value'] = params.search.value;
  delete params.search;

  return params;
}

$('table#sample').DataTable({
  'ajax': {
    'url': '/data/users',
    'type': 'GET',
    'data': flatten
  }
})
```

Back to [top](#spring-data-mongodb-datatables).


## API

The repositories now expose the following methods:

```java
DataTablesOutput<T> findAll(DataTablesInput input);
DataTablesOutput<R> findAll(DataTablesInput input, Function<T, R> converter);
DataTablesOutput<T> findAll(DataTablesInput input, Criteria additionalCriteria);

DataTablesOutput<T> findAll(DataTablesInput input, Criteria additionalCriteria,
		Criteria preFilteringCriteria);

DataTablesOutput<R> findAll(DataTablesInput input, Criteria additionalCriteria,
		Criteria preFilteringCriteria, Function<T, R> converter);
```

Your controllers should be able to handle the parameters sent by DataTables:

```java
@RestController
public class UserRestController {

  @Autowired
  private UserRepository userRepository;

  @JsonView(DataTablesOutput.View.class)
  @RequestMapping(value = "/data/users", method = RequestMethod.GET)
  public DataTablesOutput<User> getUsers(@Valid DataTablesInput input) {
    return userRepository.findAll(input);
  }

  // or with some preprocessing
  @JsonView(DataTablesOutput.View.class)
  @RequestMapping(value = "/data/users", method = RequestMethod.GET)
  public DataTablesOutput<User> getUsers(@Valid DataTablesInput input) {
    ColumnParameter parameter0 = input.getColumns().get(0);
    Specification additionalSpecification = getAdditionalSpecification(parameter0.getSearch().getValue());
    parameter0.getSearch().setValue("");
    return userRepository.findAll(input, additionalSpecification);
  }

  // or with an additional filter allowing to 'hide' data from the client (the filter will be applied on both the count and the data queries, and may impact the recordsTotal in the output)
  @JsonView(DataTablesOutput.View.class)
  @RequestMapping(value = "/data/users", method = RequestMethod.GET)
  public DataTablesOutput<User> getUsers(@Valid DataTablesInput input) {
    return userRepository.findAll(input, null, removeHiddenEntitiesSpecification);
  }
}
```

The `DataTablesInput` class maps the fields sent by the client (listed [there](https://datatables.net/manual/server-side)).

[Spring documentation](http://docs.spring.io/spring-data/jpa/docs/current/reference/html/#specifications) for `Specification`

## How to

### Apply filters

By default, the main search field is applied to all columns.

You can apply specific filter on a column with `table.columns(<your column id>).search(<your filter>).draw();` (or `table.columns(<your column name>:name)...`) (see [documentation](https://datatables.net/reference/api/columns().search())).

**Supported filters:**

* Strings (`WHERE <column> LIKE %<input>%`)
* Booleans
* Array of values (`WHERE <column> IN (<input>)` where input is something like 'PARAM1+PARAM2+PARAM4')
* `NULL` values are also supported: 'PARAM1+PARAM3+NULL' becomes `WHERE (<column> IN ('PARAM1', 'PARAM3') OR <column> IS NULL)` (to actually search for 'NULL' string, please use `\NULL`)

Also supports paging and sorting.

**Example:**

```
{
  "draw": 1,
  "columns": [
    {
      "data": "id",
      "name": "",
      "searchable": true,
      "orderable": true,
      "search": {
        "value": "",
        "regex": false
      }
    },
    {
      "data": "firstName",
      "name": "",
      "searchable": true,
      "orderable": true,
      "search": {
        "value": "",
        "regex": false
      }
    },
    {
      "data": "lastName",
      "name": "",
      "searchable": true,
      "orderable": true,
      "search": {
        "value": "",
        "regex": false
      }
    }
  ],
  "order": [
    {
      "column": 0,
      "dir": "asc"
    }
  ],
  "start": 0,
  "length": 10,
  "search": {
    "value": "john",
    "regex": false
  }
}
```

is converted into the following MongoDB query

```sql
SELECT user0_.id AS id1_0_0_,
       user0_.first_name AS first_na3_0_0_,
       user0_.last_name AS last_nam4_0_0_
FROM users user0_
WHERE (user0_.id LIKE "%john%" OR user0_.first_name LIKE "%john%" OR user0_.last_name LIKE "%john%")
ORDER BY user0_.id ASC LIMIT 10
```

### Manage non-searchable fields

If you have a column that does not match an attribute on the server-side (for example, an 'Edit' button), you'll have to set the [searchable](https://datatables.net/reference/option/columns.searchable) and [orderable](https://datatables.net/reference/option/columns.orderable) attributes to `false`.

```javascript
$(document).ready(function() {
  var table = $('table#sample').DataTable({
    'ajax' : '/data/users',
    'serverSide' : true,
    columns : [{
      data: 'id'
    }, {
      data: 'mail'
    }, {
      searchable: false,
      orderable: false
    }]
  });
}
```

### Limit the exposed attributes of the entities

There are several ways to restrict the attributes of the entities on the server-side:

- using DTO

```java
@RestController
public class UserRestController {

  @Autowired
  private UserRepository userRepository;

  @RequestMapping(value = "/data/users", method = RequestMethod.GET)
  public DataTablesOutput<UserDTO> getUsers(@Valid DataTablesInput input) {
    return userRepository.findAll(input, toUserDTO);
  }
}
```

- using `@JsonView`

```java
@Entity
public class User {

  @JsonView(DataTablesOutput.View.class)
  private Integer id;

  // ignored
  private String mail;

}

@RestController
public class UserRestController {

  @Autowired
  private UserRepository userRepository;

  @JsonView(DataTablesOutput.View.class)
  @RequestMapping(value = "/data/users", method = RequestMethod.GET)
  public DataTablesOutput<User> getUsers(@Valid DataTablesInput input) {
    return userRepository.findAll(input);
  }
}

```

- using `@JsonIgnore`

```java
@Entity
public class User {

  private Integer id;

  @JsonIgnore
  private String mail;

}
```

Back to [top](#spring-data-mongodb-datatables).


## Troubleshooting

- `Invalid property 'columns[0][data]' of bean class [org.springframework.data.jpa.datatables.mapping.DataTablesInput]`

Please see [here](#5-fix-the-serialization--deserialization-of-the-query-parameters).

- `java.lang.IllegalArgumentException: Unable to locate Attribute with the the given name ...`

It seems you have a column with a `data` attribute that does not match the attribute of the `@Entity` on the server-side.

Please see [here](#manage-non-searchable-fields).


Back to [top](#spring-data-mongodb-datatables).
