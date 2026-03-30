# Schedly Frontend

A Vue 3-based frontend for **Schedly**, a specialist consultation booking system.  
This project focuses on building a clean, role-based booking interface with modern authentication pages and scalable UI structure.

## Project Overview

Schedly is a web-based consultation booking platform designed for three main user roles:

- **Customer**
- **Specialist**
- **Administrator**

The frontend supports the core booking workflow, including:

- user authentication
- specialist browsing
- booking creation
- booking status tracking
- role-based navigation
- dashboard access for different user types

This repository currently focuses on the **frontend implementation** of the system.

---

## Tech Stack

- **Vue 3**
- **Vue Router**
- **Pinia**
- **Vite**
- **JavaScript**
- **CSS**

---

## Current Features

### Authentication Pages
- Login page
- Register page
- Dev login page for role-based testing during frontend development

### UI Features
- Custom auth page layouts
- Reusable visual style for login / register pages
- Background-image based authentication screens
- Role entry testing for Customer / Specialist / Admin

---

## Project Structure

```bash
src/
  api/              # API request logic
  assets/           # static assets
  components/       # reusable UI components
  pages/            # page-level views
    public/         # login, register, public pages
    customer/       # customer pages
    specialist/     # specialist pages
    admin/          # admin pages
  router/           # route definitions
  stores/           # Pinia stores
