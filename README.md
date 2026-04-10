# Apple Math Puzzle

A desktop math game built with Java Swing and MySQL for my Software for Enterprise module (CIS046-3).

## What is it
A math puzzle game with two modes. Memory Match where you flip cards to find matching pairs,
and Equation Challenge where you solve math puzzles fetched from an online API.
Players can register an account, track their scores, and compete on a leaderboard.

## How to run
1. Make sure XAMPP is running with MySQL on
2. Import apple_math_game.sql into phpMyAdmin
3. Add mysql-connector-j-8.4.0.jar to your project libraries in IntelliJ
4. Run Main.java

# Technologies used
 Java Swing for the UI
 MySQL for the database
 Tomato and Banana APIs for the equation puzzles

## Features
  Two game modes — Memory Match (5 levels) and Equation Challenge
  Register and login with secure password hashing
  Account lockout after 5 failed login attempts
  Guest mode for playing without an account
  Leaderboard, personal stats, and login history
  Admin panel for managing user accounts

## Notes
Email verification is simulated — no real email is sent, the token shows in a popup
Requires a local MySQL connection via XAMPP
