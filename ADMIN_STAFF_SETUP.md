# SmartQueue Admin-Created Staff Accounts

The app now uses an **admin-created staff account system**.

## How it works

Restaurant owner/admin:

1. Uses **Admin Login** from the normal login screen.
2. Opens **Staff Management**.
3. Creates a staff account with name, email and temporary password.
4. Gives the credentials to the staff member.
5. Staff uses **Staff Login** to enter the app.
6. Admin can later **Disable Staff** or **Enable Staff**.

A disabled staff account cannot log into the protected staff area because the backend disables the Firebase Authentication account as well as setting `staff/{uid}.enabled` to false.

## One-time setup

Create the restaurant owner's Firebase Authentication account manually once in:

Firebase Console → Authentication → Users → Add user

Then copy that user's UID and create this Firestore document:

```text
admins/{OWNER_UID}
  role: "admin"
  enabled: true
```

Do not give the owner access to the Firebase Console as part of normal app use. The console step is only for the initial owner/admin setup.

## Deploy the secure backend

Creating Firebase Authentication users must happen on a trusted server. The app therefore calls Firebase callable Cloud Functions; Firebase automatically includes the signed-in user's auth token with callable requests, and the backend verifies the caller before creating or disabling accounts.

From the project root, install Node.js 22 and Firebase CLI, then run:

```bash
npm install -g firebase-tools
firebase login
firebase use smartqueue-3ae01
cd functions
npm install
cd ..
firebase deploy --only functions
```

Current Firebase documentation lists Node.js 22 as a supported Cloud Functions runtime; recent Firebase Admin SDK releases also target current Node runtimes.

Cloud Functions deployment may require the Firebase/Google Cloud project to be on the Blaze plan. Check the Firebase console before deploying.

## Important

- Do not put Firebase Admin service-account credentials in the Android app.
- Do not let customers use Admin Login.
- Do not let staff use the normal customer Register screen to become staff.
- Keep the Restaurant queue logic and Firestore queue structure unchanged.
