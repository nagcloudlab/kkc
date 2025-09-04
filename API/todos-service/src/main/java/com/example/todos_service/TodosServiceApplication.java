package com.example.todos_service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

class Todo {
	private String id;
	private String title;
	private boolean completed;

	public Todo() {
	}

	public Todo(String id, String title, boolean completed) {
		this.id = id;
		this.title = title;
		this.completed = completed;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}
}

// data - layer => persisting data with databases

// service - layer => business logic
@Service
class TodosService {
	private List<Todo> todos = new ArrayList<Todo>();

	public List<Todo> getAllTodos() {
		return todos;
	}

	public void addTodo(Todo todo) {
		todos.add(todo);
	}

	public void removeTodoById(String id) {
		todos.removeIf(todo -> todo.getId().equals(id));
	}

	public void updateTodoById(String id, Todo updatedTodo) {
		for (int i = 0; i < todos.size(); i++) {
			if (todos.get(i).getId().equals(id)) {
				todos.set(i, updatedTodo);
				return;
			}
		}
	}

	public Todo getTodoById(String id) {
		for (Todo todo : todos) {
			if (todo.getId().equals(id)) {
				return todo;
			}
		}
		return null;
	}

	public void clearTodos() {
		todos.clear();
	}

}

// api/web/controller - layer => exposing endpoints ( request/response )
@RestController
class TodosController {
	// inject service layer
	private TodosService todosService;

	public TodosController(TodosService todosService) {
		this.todosService = todosService;
	}

	// define endpoints here
	// GET /todos
	// POST /todos
	@RequestMapping(method = RequestMethod.POST, path = "/todos", consumes = "application/json", produces = "application/json")
	public ResponseEntity<?> createTodo(@RequestBody Todo todo) {
		// validations
		if (todo.getTitle() == null || todo.getTitle().isEmpty()) {
			return ResponseEntity.badRequest().body("Title is required");
		}
		todosService.addTodo(todo);
		return ResponseEntity.ok().build();
	}

	// DELETE /todos/{id}
	// PUT /todos/{id}
	// GET /todos/{id}
	@RequestMapping(method = RequestMethod.GET, value = "/todos/{id}", consumes = "application/json", produces = "application/json")
	public ResponseEntity<?> getTodoById(@PathVariable String id) {
		Todo todo = todosService.getTodoById(id);
		if (todo == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(todo);
	}
	// DELETE /todos
}

@SpringBootApplication
public class TodosServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TodosServiceApplication.class, args);
	}

}
