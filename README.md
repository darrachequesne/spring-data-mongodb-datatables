[![Build Status](https://api.travis-ci.org/darrachequesne/spring-data-mongodb-datatables.svg?branch=master)](https://travis-ci.org/darrachequesne/spring-data-mongodb-datatables)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.darrachequesne/spring-data-mongodb-datatables/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.darrachequesne/spring-data-mongodb-datatables)

# spring-data-mongodb-datatables
This project is an extension of the [Spring Data MongoDB](https://github.com/spring-projects/spring-data-mongodb) project to ease its use with jQuery plugin [DataTables](http://datatables.net/) with **server-side processing enabled**.

This will allow you to handle the Ajax requests sent by DataTables for each draw of the information on the page (i.e. when paging, ordering, searching, etc.) from Spring **@RestController**.

Example:
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
}
```

## Maven dependency

```xml
<dependency>
	<groupId>com.github.darrachequesne</groupId>
	<artifactId>spring-data-mongodb-datatables</artifactId>
	<version>1.0.0</version>
</dependency>
```

## How to use

Please see the [sample project](https://github.com/darrachequesne/spring-data-jpa-datatables-sample) for a complete example. 

#### 1. Enable the use of `DataTablesRepository` factory

With either
```java
@EnableMongoRepositories(repositoryFactoryBeanClass = DataTablesRepositoryFactoryBean.class)
```
or its XML counterpart
```xml
<mongo:repositories factory-class="org.springframework.data.mongodb.datatables.DataTablesRepositoryFactoryBean" />
```

You can restrict the scope of the factory with `@EnableMongoRepositories(repositoryFactoryBeanClass = DataTablesRepositoryFactoryBean.class, basePackages = "my.package.for.datatables.repositories")`. In that case, only the repositories in the given package will be instantiated as `DataTablesRepositoryImpl` on run.

#### 2. Extend the DataTablesRepository interface

```java
public interface UserRepository extends DataTablesRepository<User, Integer> {
  ...
}
```

The `DataTablesRepository` interface extends the `MongoRepository`.

#### 3. Expose your class' attributes

```java
public class User {

	@JsonView(DataTablesOutput.View.class)
	private Integer id;

	@JsonView(DataTablesOutput.View.class)
	private String mail;

	// not exposed
	private String hiddenField;

	@ManyToOne
	@JoinColumn(name = "id_address")
	@JsonView(DataTablesOutput.View.class)
	private Address address;

}
```

#### 4. Include jquery.spring-friendly.js

It overrides jQuery data serialization to allow Spring MVC to correctly map input parameters (by changing `column[0][data]` to `column[0].data` in request payload)

#### On the server-side

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

#### On the client-side

On the client-side, you can now define your table loading data dynamically :

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
		}, {
			// add another column which will not be persisted on the server-side
			data : 'anothercolumn',
			// ordering and filtering are not available
			// (but could be implemented with additional specifications)
			orderable : false,
			searchable : false,
			render : function(data, type, row) {
				return row.id ? 'Your id is ' + row.id : '';
			}
		}]
	});
}
```

**Note:** You can also retrieve data through POST requests with:

```javascript
$(document).ready(function() {
	var table = $('table#sample').DataTable({
		'ajax': {
			'contentType': 'application/json',
			'url': '/data/users',
			'type': 'POST',
			'data': function(d) {
				return JSON.stringify(d);
			}
		},
		...
```
```java
// and server-side becomes
@JsonView(DataTablesOutput.View.class)
@RequestMapping(value = "/data/users", method = RequestMethod.POST)
public DataTablesOutput<User> getUsers(@Valid @RequestBody DataTablesInput input) {
	return userRepository.findAll(input);
}
```

In that case, including `jquery.spring-friendly.js` is not necessary.
