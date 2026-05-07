// Test module resolution
import("@printflow/auth").then(m => {
  console.log("Available exports:", Object.keys(m));
  console.log("AuthError:", m.AuthError);
}).catch(e => console.error("Error:", e.message));
