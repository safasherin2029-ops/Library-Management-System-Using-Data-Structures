# Library Management System Using Data Structures

A beginner-friendly Java mini project demonstrating **Stack (LIFO)** and **Queue (FIFO)** with a small HTML/CSS/JavaScript web interface.

## Features
- Add a returned book to the returned-books stack (**Push**).
- Process the most recently returned book (**Pop**).
- View returned books in stack order.
- Add students to the waiting list (**Enqueue**).
- Serve the student at the front of the queue (**Dequeue**).
- View the waiting list in arrival order.
- Java backend with no third-party libraries.

## Requirements
- Java Development Kit (JDK) 17 or later
- A modern web browser

Check Java is installed:
```bash
java -version
javac -version
```

## Run the project

Open a terminal in the project folder and run:

### Windows
```bat
run.bat
```

### macOS / Linux
```bash
chmod +x run.sh
./run.sh
```

Alternatively, compile and run manually from the project folder:

```bash
javac -d out src/LibraryManagementServer.java
java -cp out LibraryManagementServer
```

Then open **http://localhost:8080** in your browser. Keep the terminal open while using the application. Press `Ctrl+C` in the terminal to stop the server.

## Project structure
```text
Library_Management_System/
├── src/
│   └── LibraryManagementServer.java
├── public/
│   ├── index.html
│   ├── style.css
│   └── script.js
├── run.bat
├── run.sh
└── README.md
```

## Data structures used

### 1. Stack — returned books
The stack follows **Last-In, First-Out (LIFO)**. The most recently added returned book is processed first. The Java code uses `java.util.Stack` and supports push, pop, and display operations.

### 2. Queue — students waiting to borrow books
The queue follows **First-In, First-Out (FIFO)**. The student who joins first is served first. The Java code uses `java.util.Queue` with `java.util.LinkedList` and supports enqueue, dequeue, and display operations.

## API endpoints
- `GET /api/books` — display returned-books stack
- `POST /api/books` — push a returned book; JSON body: `{"book":"Book title"}`
- `POST /api/books/pop` — pop the most recently returned book
- `GET /api/waitlist` — display student queue
- `POST /api/waitlist` — enqueue a student; JSON body: `{"student":"Student name"}`
- `POST /api/waitlist/dequeue` — serve/remove the first student

## Notes
- This is a mini-project demonstration. The stack and queue are kept in memory, so the data resets when the Java server is restarted.
- Run the server on your own computer; the web page communicates with the local Java server.
