// Force unbuffered output
const m = await import("@printflow/auth");
const keys = Object.keys(m);
process.stdout.write("Keys: " + JSON.stringify(keys) + "\n");
if (m.AuthError) {
  process.stdout.write("AuthError: EXISTS\n");
} else {
  process.stdout.write("AuthError: MISSING\n");
}
