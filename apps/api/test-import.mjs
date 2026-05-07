// Test module resolution
async function test() {
  try {
    const m = await import("@printflow/auth");
    console.log("Available exports:", Object.keys(m));
    console.log("AuthError:", m.AuthError);
  } catch (e) {
    console.error("Error:", e.message);
    process.exit(1);
  }
}
test();
