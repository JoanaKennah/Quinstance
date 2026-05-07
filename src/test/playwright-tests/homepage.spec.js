// @ts-check
import { test, expect } from '@playwright/test';

test('Welcome Page has title "Quinstance"', async ({ page }) => {
  await page.goto('http://localhost:8080');

  await expect(page).toHaveTitle(/Quinstance/);
});

test('Welcome Page displays text', async ({ page }) => {
    await page.goto('http://localhost:8080');

    await expect(page.locator('main')).toHaveText;
});

test('Welcome Page "How to Play" button takes user to How to Play Page', async ({ page }) => {
    await page.goto('http://localhost:8080');

    // Click 'How to Play' button
    await page.getByRole('button', { name: 'How to Play'}).click();

    await expect(page).toHaveTitle(/How to Play/);
    await expect(page.locator('column')).toHaveText;
});

test('Welcome Page "Login" button takes user to Login Page', async ({ page }) => {
    await page.goto('http://localhost:8080');

    // Click 'Log In' button
    await page.getByRole('button', { name: 'Log In'}).click();

    await expect(page).toHaveTitle(/Login/);
    await expect(page.locator('switch-text')).toHaveText;
});

test('Welcome Page "Sign Up" button takes user to Sign Up Page', async ({ page }) => {
    await page.goto('http://localhost:8080');

    // Click 'Sign Up' button
    await page.getByRole('button', { name: 'Sign Up'}).click();

    await expect(page).toHaveTitle(/Sign Up/);
    await expect(page.locator('switch-text')).toHaveText;
});