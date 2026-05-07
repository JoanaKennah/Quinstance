// @ts-check
import { test, expect } from '@playwright/test';

// just checking host flow
test('Host can create a lobby and sees room code', async ({ browser }) => {
    // uses browser instead of page to create multiple contexts for game
    const hostContext = await browser.newContext();
    const hostPage = await hostContext.newPage();

    // host login
    await hostPage.goto('/login');
    await hostPage.getByLabel('Username:').fill('JoanaKennah');
    await hostPage.getByLabel('Password:').fill('password456');
    await hostPage.getByRole('button', { name: 'Enter' }).click();
    await expect(hostPage).toHaveURL(/teacher-dashboard/);

    // host navigates to quiz and creates lobby
    await hostPage.goto('/view-quizzes');
    await hostPage.getByText('COMP2870 Quiz').click();
    await hostPage.getByRole('button', { name: 'Create a Lobby' }).click();
    await hostPage.getByRole('button', { name: 'Create Lobby!' }).click();

    // check host is on lobby server page with room code visible
    await expect(hostPage).toHaveURL(/lobby-server/);
    await expect(hostPage.getByText('Room Code:')).toBeVisible();

    await hostContext.close();
});

// checking combined lobby flow host and client
test('Client can join lobby with valid room code and appears in player list', async ({ browser }) => {
    // create two separate contexts, one host one client
    const hostContext = await browser.newContext();
    const clientContext = await browser.newContext({ storageState: { cookies: [], origins: [] } });
    const hostPage = await hostContext.newPage();
    const clientPage = await clientContext.newPage();

    // host login and lobby creation
    await hostPage.goto('/login');
    await hostPage.getByLabel('Username:').fill('JoanaKennah');
    await hostPage.getByLabel('Password:').fill('password456');
    await hostPage.getByRole('button', { name: 'Enter' }).click();
    // wait for host to fully land on dashboard before starting client
    await hostPage.waitForURL(/teacher-dashboard/);
    await hostPage.goto('/view-quizzes');
    await hostPage.getByText('COMP2870 Quiz').click();
    await hostPage.getByRole('button', { name: 'Create a Lobby' }).click();
    await hostPage.getByRole('button', { name: 'Create Lobby!' }).click();
    await expect(hostPage).toHaveURL(/lobby-server/);

    // get room code from host page
    const roomCodeText = await hostPage.getByText(/Room Code:/).textContent();
    const roomCode = roomCodeText.replace('Room Code: ', '').trim();

    // client login, joins with room code
    // using pressSequentially to prevent webkit autofill clearing fields
    await clientPage.goto('/login');
    await clientPage.getByLabel('Username:').click();
    await clientPage.getByLabel('Username:').pressSequentially('linguin0');
    await clientPage.getByLabel('Password:', { exact: true }).click();
    await clientPage.getByLabel('Password:', { exact: true }).pressSequentially('password123');
    await clientPage.getByRole('button', { name: 'Enter' }).click();
    await clientPage.waitForURL('**/*');
    console.log('Client landed on:', clientPage.url());
    await clientPage.goto('/join-lobby');

    // added wait for load for webkit functionality
    await clientPage.waitForLoadState('networkidle');
    await clientPage.getByLabel('Room Code').fill(roomCode);
    await clientPage.getByRole('button', { name: 'Join' }).click();
    await expect(clientPage).toHaveURL(/lobby-client/);

    // host should see client in player list
    await expect(hostPage.getByText('linguin0')).toBeVisible();

    await hostContext.close();
    await clientContext.close();
});

// client attempt to join lobby w/ invalid code - only needs 1 context
test('Client cannot join lobby with invalid room code', async ({ page }) => {
    // client login
    await page.goto('/login');
    await page.getByLabel('Username:').fill('linguin0');
    await page.getByLabel('Password:').fill('password123');
    await page.getByRole('button', { name: 'Enter' }).click();

    // navigate to join lobby, enter invalid room code
    await page.goto('/join-lobby');
    // added wait for load for webkit functionality
    await page.waitForLoadState('networkidle');
    await page.getByLabel('Room Code').fill('XXXXXXXX');
    await page.getByRole('button', { name: 'Join' }).click();

    // client should stay on join lobby page w/ error message
    await expect(page).toHaveURL(/join-lobby/);
    await expect(page.getByText('Invalid room code')).toBeVisible();
});

// client leaving lobby - needs two contexts
test('Client can leave lobby and is removed from player list', async ({ browser }) => {
    const hostContext = await browser.newContext();
    const clientContext = await browser.newContext();
    const hostPage = await hostContext.newPage();
    const clientPage = await clientContext.newPage();

    // host login & lobby creation
    await hostPage.goto('/login');
    await hostPage.getByLabel('Username:').fill('JoanaKennah');
    await hostPage.getByLabel('Password:').fill('password456');
    await hostPage.getByRole('button', { name: 'Enter' }).click();
    // wait for host to fully land on dashboard before starting client
    await hostPage.waitForURL(/teacher-dashboard/);
    await hostPage.goto('/view-quizzes');
    await hostPage.getByText('COMP2870 Quiz').click();
    await hostPage.getByRole('button', { name: 'Create a Lobby' }).click();
    await hostPage.getByRole('button', { name: 'Create Lobby!' }).click();

    // client joins
    const roomCodeText = await hostPage.getByText(/Room Code:/).textContent();
    const roomCode = roomCodeText.replace('Room Code: ', '').trim();
    // using pressSequentially to prevent webkit autofill clearing fields
    await clientPage.goto('/login');
    await clientPage.getByLabel('Username:').click();
    await clientPage.getByLabel('Username:').pressSequentially('linguin0');
    await clientPage.getByLabel('Password:', { exact: true }).click();
    await clientPage.getByLabel('Password:', { exact: true }).pressSequentially('password123');
    await clientPage.getByRole('button', { name: 'Enter' }).click();
    await clientPage.goto('/join-lobby');
    // added wait for load for webkit functionality
    await clientPage.waitForLoadState('networkidle');
    await clientPage.getByLabel('Room Code').fill(roomCode);
    await clientPage.getByRole('button', { name: 'Join' }).click();
    await expect(clientPage).toHaveURL(/lobby-client/);

    // client leaves lobby
    await clientPage.getByRole('button', { name: 'Leave Lobby' }).click();
    await expect(clientPage).toHaveURL(/student-dashboard/);

    // host should no longer see client in player list
    await expect(hostPage.getByText('linguin0')).not.toBeVisible();

    await hostContext.close();
    await clientContext.close();
});

//host kicks client - two contexts
test('Host can kick client from lobby', async ({ browser }) => {
    const hostContext = await browser.newContext();
    const clientContext = await browser.newContext();
    const hostPage = await hostContext.newPage();
    const clientPage = await clientContext.newPage();

    // host login and lobby creation
    await hostPage.goto('/login');
    await hostPage.getByLabel('Username:').fill('JoanaKennah');
    await hostPage.getByLabel('Password:').fill('password456');
    await hostPage.getByRole('button', { name: 'Enter' }).click();
    // wait for host to fully land on dashboard before starting client
    await hostPage.waitForURL(/teacher-dashboard/);
    await hostPage.goto('/view-quizzes');
    // added wait for load for webkit functionality
    await hostPage.waitForLoadState('networkidle');
    await hostPage.getByText('COMP2870 Quiz').click();
    await hostPage.getByRole('button', { name: 'Create a Lobby' }).click();
    await hostPage.getByRole('button', { name: 'Create Lobby!' }).click();

    // client joins
    const roomCodeText = await hostPage.getByText(/Room Code:/).textContent();
    const roomCode = roomCodeText.replace('Room Code: ', '').trim();
    // using pressSequentially to prevent webkit autofill clearing fields
    await clientPage.goto('/login');
    await clientPage.getByLabel('Username:').click();
    await clientPage.getByLabel('Username:').pressSequentially('linguin0');
    await clientPage.getByLabel('Password:', { exact: true }).click();
    await clientPage.getByLabel('Password:', { exact: true }).pressSequentially('password123');
    await clientPage.getByRole('button', { name: 'Enter' }).click();
    await clientPage.goto('/join-lobby');
    // added wait for load for webkit functionality
    await clientPage.waitForLoadState('networkidle');
    await clientPage.getByLabel('Room Code').fill(roomCode);
    await clientPage.getByRole('button', { name: 'Join' }).click();
    await expect(clientPage).toHaveURL(/lobby-client/);

    // host navigates to manage participants & kicks client
    await hostPage.getByRole('button', { name: 'Settings' }).click();
    await hostPage.getByRole('button', { name: 'Manage Participants' }).click();
    await hostPage.getByText('linguin0').click();
    await hostPage.getByRole('button', { name: 'Kick Selected' }).click();
    await hostPage.getByRole('button', { name: 'Save' }).click();

    // client should be redirected to kicked page
    await expect(clientPage).toHaveURL(/kicked/);

    await hostContext.close();
    await clientContext.close();
});