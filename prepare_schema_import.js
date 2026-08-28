const fs = require("fs");
const path = require("path");

const input = path.join(__dirname, "schema_tables.txt");
const output = path.join(__dirname, "import_schema_tables.sql");
const text = fs.readFileSync(input, "utf8");
const lines = text.split(/\r?\n/);

const sqlLines = [
  "DROP DATABASE IF EXISTS `fiva_assistant`;",
  "CREATE DATABASE `fiva_assistant` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;",
  "USE `fiva_assistant`;",
  "SET NAMES utf8mb4;",
  "SET FOREIGN_KEY_CHECKS = 0;",
  "",
];

let collectingCreate = false;
let collectingInsert = false;

for (const line of lines) {
  const trimmed = line.trim();

  if (trimmed.startsWith("CREATE TABLE")) {
    collectingCreate = true;
    collectingInsert = false;
  }

  if (trimmed.startsWith("INSERT ")) {
    collectingInsert = true;
    collectingCreate = false;
  }

  if (collectingCreate || collectingInsert) {
    sqlLines.push(line.replace(/utf8mb4_0900_ai_ci/g, "utf8mb4_unicode_ci"));
  }

  if ((collectingCreate || collectingInsert) && trimmed.endsWith(";")) {
    collectingCreate = false;
    collectingInsert = false;
    sqlLines.push("");
  }
}

sqlLines.push("SET FOREIGN_KEY_CHECKS = 1;");
fs.writeFileSync(output, sqlLines.join("\n"), "utf8");
console.log(output);
