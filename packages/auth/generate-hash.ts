import { hashPassword } from "./src/password.js";

const adminHash = await hashPassword("admin");
const demoHash = await hashPassword("demo");

console.log("Admin password hash for 'admin':", adminHash);
console.log("Demo password hash for 'demo':", demoHash);
