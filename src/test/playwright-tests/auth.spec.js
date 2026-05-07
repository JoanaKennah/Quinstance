// @ts-check
import { test, expect } from '@playwright/test';

//testing teacher authentication route
test('login w/ valid teacher creds redirects to teacher dashboard', async ({ page }) => {
    // navigate to login page
    await page.goto('/login');

    // find username input by label, fill
    await page.getByLabel('Username:').fill('JoanaKennah');

    // find password input by label, fill
    await page.getByLabel('Password:').fill('password456');

    // find and submit info
    await page.getByRole('button', { name: 'Enter' }).click();

    // wait until navigation finishes then check landing
    await expect(page).toHaveURL(/teacher-dashboard/);
    await expect(page).toHaveTitle(/Dashboard/);
});

test('Login w/ invalid password shows error message', async ({ page }) => {
    // navigate to login page
    await page.goto('/login');

    // fill in valid username but wrong password
    await page.getByLabel('Username:').fill('JoanaKennah');
    await page.getByLabel('Password:').fill('wrongpassword');

    await page.getByRole('button', { name: 'Enter' }).click();

    // page should not redirect, should stay & show error
    await expect(page).toHaveURL(/login/);

    // check error message present & visible
    await expect(page.getByText('Incorrect password')).toBeVisible();
});

//testing student login
test('Login w/ valid student creds redirects to student dashboard', async ({ page }) => {
    // navigate to login page
    await page.goto('/login');

    await page.getByLabel('Username:').fill('linguin0');
    await page.getByLabel('Password:').fill('password123');

    await page.getByRole('button', { name: 'Enter' }).click();

    // check student lands on correct dash
    await expect(page).toHaveURL(/student-dashboard/);
    await expect(page).toHaveTitle(/Dashboard/);
});

test('Signup w/ mismatched passwords shows error', async ({ page }) => {
    // navigate to signup page
    await page.goto('/signup');

    // fill signup fields
    await page.getByLabel('First Name:').fill('Test');
    await page.getByLabel('Last Name:').fill('User');
    await page.getByLabel('Email Address:').fill('test@test.com');
    await page.getByLabel('Username:').fill('spiritscout');
    await page.getByLabel('Password:', { exact: true }).fill('password123');
    await page.getByLabel('Confirm Password:').fill('password456');

    await page.getByRole('button', { name: 'Enter' }).click();

    // caught mismatch so stays on signup page
    await expect(page).toHaveURL(/signup/);
});

test('Signup w/ valid student data redirects to student dashboard', async ({ page }) => {
    // navigate to signup page
    await page.goto('/signup');

    await page.getByLabel('First Name:').fill('Spirit');
    await page.getByLabel('Last Name:').fill('Scout');
    await page.getByLabel('Email Address:').fill('spiritscout@test.com');
    await page.getByLabel('Username:').fill('spiritscout');
    await page.getByLabel('Password:', { exact: true }).fill('password123');
    await page.getByLabel('Confirm Password:').fill('password123');

    // select role & keystage dropdowns
    await page.getByLabel('Role:').selectOption('student');
    await page.getByLabel('Key Stage:').selectOption('furthereducation');

    await page.getByRole('button', { name: 'Enter' }).click();

    // should redirect to student dashboard if successful signup
    await expect(page).toHaveURL(/student-dashboard/);
});

// deletes spiritscout user so tests can be reran
test.afterAll(async ({ request }) => {
    await request.delete('/user/spiritscout');
});