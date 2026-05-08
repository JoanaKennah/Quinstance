# Welcome to Quinstance!
## Table of Contents
1. [Project Information](#project-information)
2. [Running our Application](#running-our-application)
3. [Logging In](#logging-in)
4. [Running Tests](#running-tests)

## Project Information
### Who we are
This is Games Night #10's group project, consisting of four members:
* Emily Williams [ spdx5651@leeds.ac.uk ]
* Joana Kennah   [ clhd4492@leeds.ac.uk ]
* Ethan Harris   [ jzfl2265@leeds.ac.uk ]
* Minghao Le     [ tgqj4326@leeds.ac.uk ] 

## Running our Application
To run the Quinstance application, enter this command into the terminal:
`./gradlew clean run`

## Logging In
We have created a few accounts for you to explore. Each teacher will have a quiz or two, which their students will have
played once or twice! 

### Classroom 1: Ancient History
Teacher - Ethan Harris
* **Username**: CoolTeacher123
* **Password**: secure password

Student 1 - Hugh Morris
* **Username**: HughMorris123
* **Password**: laughs

Student 2 - Bubbles the Clown
* **Username**: BubblesTheClown 
* **Password**: hehe

Student 3 - The Observer
* **Username**: TheObserver
* **Password**: watching

### Classroom 2: Maths
Teacher - Joana Kennah
* **Username**: JoanaK
* **Password**: teacher

Student 1 - Theo (Joana's friend)
* **Username**: JoanasFriendTheo
* **Password**: child2

Student 2 - Rory M
* **Username**: RoryM
* **Password**: child3

## Running Tests
We have created two kinds of tests for our project. These include **Unit tests** using Kotest and **Integration tests** using
Playwright.

### Kotest
To run our Kotest tests, please run `./gradlew check`. This will not only run all the Kotest tests but also run a **Ktlint
check** also on our Main project files and Test files!

### Playwright
To run our Playwright tests, please run `npx playwright test --workers=1`

NOTE: You may need to install playwright before running the command above. If so, please run `npm install playwright`. 

