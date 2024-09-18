const express = require('express');
const mongoose = require("mongoose");
const server = "127.0.0.1:27017";
const database = "DB1";
const cors = require('cors');
const fs = require('fs');
const app = express();
const port = 3000;

app.use(cors());
app.use(express.json());

app.get('/api/data', (req, res) => {
  fs.readFile('data.json', 'utf8', (err, data) => {
    if (err) {
      res.status(500).json({ error: 'Error reading data file' });
    } else {
      res.json(JSON.parse(data));
    }
  });
});

app.post('/api/data', (req, res) => {
  const newData = req.body;
  fs.writeFile('data.json', JSON.stringify(newData, null, 2), (err) => {
    if (err) {
      res.status(500).json({ error: 'Error writing data file' });
    } else {
      res.json({ message: 'Data saved successfully' });
    }
  });
});

app.listen(port, () => {
  console.log(`Server is running on port ${port}`);
});

class Database {
  constructor() {
    this._connect();
  }
  _connect() {
    mongoose
      .connect(`mongodb://${server}/${database}`)
      .then(() => {
        console.log("Database connection successful");
      })
      .catch((err) => {
        console.error("Database connection failed");
      });
  }
}

module.exports = new Database();

//define the schema database of mongoose
const userSchema = new mongoose.Schema({
  id: String,
  name: String,
  price: String,
  desc: String,
  color: String,
  url: String,
  isMarked: Boolean,
});

// Using schema to build Mongoose models
const User = mongoose.model("data", userSchema);

// performing POST and GET endpoints using Express framework
app.post("/api/postMongo", async (request, response) => {
  const user = await User.create(request.body);
  console.log("Received POST data:", request.body);
  try {
    await user.save();
    response.send(user);
  } catch (error) {
    response.status(500).send(error);
  }
});
app.get("/api/getMongo", async (request, response) => {
  const users = await User.find({});
  try {
    response.send(users);
  } catch (error) {
    response.status(500).send(error);
  }
});