import express from "express";
import bodyParser from "body-parser";
import { render } from "ejs";
import fs from "fs";
var index = 0;
var blogs = [];
var title = [];
const port = 3000;
const app = express();
app.use(bodyParser.urlencoded({ extended: true }));
app.use(express.static("public"));
app.get("/", (req, res) => {
  res.render("index.ejs", {
    blog: blogs,
    title: title,
  });
  console.log(index);
});
app.post("/eassy", (req, res) => {
  index = req.body["myHiddenInput"];
  res.render("editePage.ejs", {
    title: title[index],
    blog: blogs[index],
  });
});
app.post("/eds", (req, res) => {
  title[index] = req.body["title"];
  blogs[index] = req.body["blog"];
  res.render("index.ejs", {
    blog: blogs,
    title: title,
  });
});
app.post("/edit", (req, res) => {
  res.render("edit.ejs", {
    title: title[index],
    blog: blogs[index],
  });
});
app.get("/about", (req, res) => {
  res.render("about.ejs");
});
app.get("/contact", (req, res) => {
  res.render("contact.ejs");
});
app.get("/Post", (req, res) => {
  res.render("post.ejs");
});
app.get("/submit", (req, res) => {
  app.get("/", (req, res) => {
    res.render("index.ejs", {
      blog: blogs,
      title: title,
    });
  });
});
app.post("/submit", (req, res) => {
  blogs.push(req.body["blog"]);
  title.push(req.body["title"]);
 
});
app.listen(port, () => {
  console.log("the serever listing on port:" + port);
});
app.post("/delete", (req, res) => {
  title.splice(index, 1);
  blogs.splice(index, 1);
  res.render("index.ejs", {
    blog: blogs,
    title: title,
  });
});
