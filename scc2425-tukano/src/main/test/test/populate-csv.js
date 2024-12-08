'use strict';

const fs = require('fs');
const path = require('path');

// Characters for generating usernames and passwords
const letters = 'abcdefghijklmnopqrstuvwxyz';
const specialChars = '!@#$%^&*()_+-=[]{}|;:.<>?/';
const allChars = letters + letters.toUpperCase() + specialChars + '0123456789';

// Function to generate random usernames
function randomUsername(length) {
    let username = '';
    for (let i = 0; i < length; i++) {
        username += letters[Math.floor(Math.random() * letters.length)];
    }
    return username;
}

// Function to generate random passwords
function randomPassword(length) {
    let password = '';
    for (let i = 0; i < length; i++) {
        password += allChars[Math.floor(Math.random() * allChars.length)];
    }
    return password;
}

// Function to create random users
function generateUsers(count) {
    const users = [];
    for (let i = 0; i < count; i++) {
        const username = randomUsername(Math.floor(Math.random() * 6) + 5); // 5-10 characters
        const password = randomPassword(Math.floor(Math.random() * 6) + 10); // 10-15 characters
        const email = `${username}@fct.unl.pt`;
        const displayName = `${username.charAt(0).toUpperCase()}${username.slice(1)}`;
        users.push({ userid: username, pwd: password, email: email, displayName: displayName });
    }
    return users;
}

// Function to write users to a CSV file
function writeUsersToCsv(filePath, users) {
    const header = 'userid,pwd,email,displayName\n';
    const rows = users.map(user => `${user.userid},${user.pwd},${user.email},"${user.displayName}"`).join('\n');
    const csvData = header + rows;

    // Clear the file before writing new data
    fs.writeFileSync(filePath, '', 'utf8');

    // Write the new data
    fs.writeFileSync(filePath, csvData, 'utf8');
    console.log(`CSV file created at: ${filePath}`);
}

// Main execution
const outputPath = path.join(__dirname, 'data/usersTest.csv');
const userCount = 50; // Generate 30 users

// Ensure the data directory exists
fs.mkdirSync(path.dirname(outputPath), { recursive: true });

const users = generateUsers(userCount);
writeUsersToCsv(outputPath, users);
