# FaceAuthSecureVault

A secure file-vault web application with *face-recognition-based login*, built using Spring Boot. Users register, enroll their face, and can then log in either with a password or by scanning their face. Once inside, they can upload, download, and manage personal files, with automatic email notifications for logins and uploads.

---

## Features

- *User Registration & Login* — standard username/password authentication
- *Face Recognition Login* — users enroll their face once and can log in by scanning it
- *File Vault* — upload, download, view, and delete personal files, stored per user
- *Email Notifications* — automatic email alerts sent to the user on:
  - Successful login
  - Successful file upload
- *Dashboard* — overview screen showing security score, face ID status, login history, and uploaded files

---

## Tech Stack

*Backend*
- Java, Spring Boot
- Spring Data JPA / Hibernate
- MySQL
- Spring Mail (JavaMailSender) for email notifications
- OpenCV / Haar Cascade (haarcascade_frontalface_default.xml) for face detection

*Frontend*
- HTML, CSS, JavaScript (vanilla, no framework)

*Build Tool*
- Maven

---

## Project Structure


src/main/java/com/faceauth/faceauth/
 ├── controller/     → REST endpoints (Auth, FaceAuth, File, User)
 ├── entity/         → JPA entities (User, FileEntity, FaceEmbedding, FaceRequest)
 ├── repository/     → Spring Data JPA repositories
 └── service/        → Business logic (EmailService, FaceService, FileService)

src/main/resources/
 ├── static/         → Frontend pages (login, register, dashboard, enroll)
 ├── application-sample.properties → Template config (copy to application.properties)
 └── haarcascade_frontalface_default.xml → Face detection model


---

## How to Run Locally

### Prerequisites
- Java 17+
- Maven
- MySQL Server running locally

### Setup

1. *Clone the repository*
   bash
   git clone https://github.com/deepak-016/faceauthsecurevault.git
   cd faceauthsecurevault
   

2. *Create the database*
   sql
   CREATE DATABASE faceauth;
   

3. *Configure your local settings*
   Copy src/main/resources/application-sample.properties to
   src/main/resources/application.properties, then fill in your own values:
   properties
   spring.datasource.username=your_mysql_username
   spring.datasource.password=your_mysql_password

   spring.mail.username=your_email@gmail.com
   spring.mail.password=your_gmail_app_password
   
   > Use a [Gmail App Password](https://myaccount.google.com/apppasswords) (not your regular Gmail password) for email to work. application.properties is git-ignored and never committed — this keeps real credentials out of the repo.

4. *Run the app*
   bash
   ./mvnw spring-boot:run
   

5. *Open in browser*
   
   http://localhost:8086
   
   Register an account, enroll your face, then try logging in both by password and by face scan.

---

## Screenshots

(Add screenshots or a short screen-recording GIF of the login, dashboard, and face enrollment screens here.)

---

## Notes

- Face recognition requires camera access, which browsers only permit on localhost or over HTTPS.
- Email notifications fail gracefully — if mail sending fails (e.g. bad credentials), login/upload still succeeds; the error is only logged to the server console.
