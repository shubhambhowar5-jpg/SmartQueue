# SmartQueue Admin-Created Staff Accounts — Free Spark Plan

This version keeps the staff account system on Firebase Authentication + Firestore and does not require Cloud Functions or the Blaze plan.

## One-time Firebase setup

1. Firebase Console → Authentication → Sign-in method → enable **Email/Password**.
2. Firebase Console → Authentication → Users → **Add user**.
3. Create the first Admin account. Copy its **UID**.
4. Firestore Database → Data → create collection `admins`.
5. Create a document whose Document ID is the Admin UID.
6. Add:

```text
role     = admin    (string)
enabled  = true     (boolean)
```

The owner/admin uses **Admin Login** in SmartQueue. Firebase Console is only needed for this initial admin setup.

## Staff flow

```text
Admin Login
  → Staff Management
  → Add Staff
  → enter staff name + invited email
  → SmartQueue creates a 6-digit invitation code
  → give the email + code to the staff member

Staff Login
  → Staff Registration
  → enter invited email + code
  → choose password
  → Firebase Authentication creates the account
  → SmartQueue creates staff/{UID}
  → invitation becomes CLAIMED
  → staff can use Staff Login
```

The admin can later enable or disable the staff document using the Staff Management screen.

## Firestore collections

```text
admins/{ADMIN_UID}
  role: "admin"
  enabled: true

staffInvites/{6_DIGIT_CODE}
  name: "Staff Name"
  email: "staff@example.com"
  role: "staff"
  enabled: true
  status: "INVITED"
  createdBy: "ADMIN_UID"

staff/{STAFF_UID}
  uid: "STAFF_UID"
  name: "Staff Name"
  email: "staff@example.com"
  role: "staff"
  enabled: true
  status: "ACTIVE"
  inviteCode: "6_DIGIT_CODE"
  createdBy: "ADMIN_UID"
```

No Cloud Functions are used by this version, and no Firebase Admin/service-account credentials belong in the Android app.
