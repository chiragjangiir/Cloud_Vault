CLOUD VAULT

**To run this project:**

**Prerequisites:**
    * Java Development Kit (JDK) 17 or higher.
    * Maven (usually comes with IDEs like IntelliJ IDEA or Eclipse).
    * An IDE (IntelliJ IDEA Community Edition is excellent for Spring Boot).

Click "Generate" and download the ZIP file.
    * **Unzip and Open in IDE:** Extract the downloaded ZIP and open the project in your IDE (e.g., IntelliJ IDEA: `File -> Open -> select the unzipped folder`).
    * **Create Folders and Files:**
        * Inside `src/main/java/com/example/cloudstorage/`, create new packages: `config`, `model`, `repository`, `service`, `controller`.
        * Place the `.java` files into their respective packages as indicated by the comments in the code.
        * Create `src/main/resources/application.properties` and paste its content.
        * Create `src/main/resources/templates/` and place `index.html` and `dashboard.html` inside it.
    * **Run the Application:**
        * Navigate to `src/main/java/com/example/cloudstorage/CloudStorageApplication.java`.
        * Right-click on the `main` method and select "Run 'CloudStorageApplication.main()'".

 **Access the Application:**
    * Once the application starts (you'll see messages in the console indicating it's running on port 8080), open your web browser and go to: `http://localhost:8080/`

**Explanation and Next Steps:**

* **`CloudStorageApplication.java`**: The main entry point for the Spring Boot application.
* **`StorageProperties.java`**: A simple class to externalize the file storage location. This makes it easy to change where files are saved without modifying code.
* **`User.java` and `FileMetadata.java`**: These are JPA (Java Persistence API) entities. They represent tables in your database and how data is structured. `FileMetadata` has a `ManyToOne` relationship with `User`, meaning many files can belong to one user.
* **`UserRepository.java` and `FileMetadataRepository.java`**: These are Spring Data JPA repositories. Spring Data JPA automatically generates the necessary database access code (CRUD operations) for you based on the entity and method names.
* **`StorageService.java`**: This is the core business logic for handling file operations. It interacts with the `FileMetadataRepository` to save/retrieve metadata and uses `java.nio.file` to interact with the local file system. It also generates a `uniqueFilename` using `UUID` to prevent name collisions.
* **`AuthController.java`**: Manages user registration and login. For simplicity, it uses a basic session attribute (`loggedInUser`). **Important:** In a real application, you would use a robust security framework like Spring Security to handle password hashing, session management, and authorization properly.
* **`FileController.java`**: Handles HTTP requests related to file management (upload, list, download, delete). It uses `MultipartFile` for uploads and `ResponseEntity` for downloads.
* **`application.properties`**: Configures the H2 in-memory database (data is lost when the application stops, but it's great for development) and tells Spring Boot where to store uploaded files (`uploads` folder in your project root).
* **`index.html`**: The landing page for login and registration. It uses basic JavaScript to toggle between the login and registration forms.
* **`dashboard.html`**: The main page after a user logs in, showing their files and providing upload/download/delete options. It uses Thymeleaf to dynamically display data from the backend.
