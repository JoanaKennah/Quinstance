// @ts-check
import { test, expect } from '@playwright/test';

// Note for future testing ---
// These tests need to be run with --workers=1 because sharing server state breaks them.
// This is purely test architecture and does not affect a real scenario

test.setTimeout(120000); // 2 minutes to allow for game flow

// tests full game flow - from singing in and lobby creation to leaderboard/end-game
test('Full game flow from start to end screen', async ({ browser }) => {
    const hostContext = await browser.newContext();
    const clientContext = await browser.newContext({ storageState: { cookies: [], origins: [] } });
    const hostPage = await hostContext.newPage();
    const clientPage = await clientContext.newPage();

    // host login & lobby creation
    await hostPage.goto('/login');
    await hostPage.getByLabel('Username:').fill('JoanaKennah');
    await hostPage.getByLabel('Password:').fill('password456');
    await hostPage.getByRole('button', { name: 'Enter' }).click();
    await hostPage.waitForURL(/teacher-dashboard/);
    await hostPage.goto('/view-quizzes');
    await hostPage.getByText('COMP2870 Quiz').click();
    await hostPage.getByRole('button', { name: 'Create a Lobby' }).click();
    await hostPage.getByRole('button', { name: 'Create Lobby!' }).click();
    await hostPage.waitForURL(/lobby-server/);

    // get room code
    const roomCodeText = await hostPage.getByText(/Room Code:/).textContent();
    const roomCode = roomCodeText.replace('Room Code: ', '').trim();

    // client login & join
    await clientPage.goto('/login');
    await clientPage.getByLabel('Username:').click();
    await clientPage.getByLabel('Username:').pressSequentially('linguin0');
    await clientPage.getByLabel('Password:', { exact: true }).click();
    await clientPage.getByLabel('Password:', { exact: true }).pressSequentially('password123');
    await clientPage.getByRole('button', { name: 'Enter' }).click();
    await clientPage.goto('/join-lobby');
    await clientPage.waitForLoadState('networkidle');
    await clientPage.getByLabel('Room Code').fill(roomCode);
    await clientPage.getByRole('button', { name: 'Join' }).click();
    await expect(clientPage).toHaveURL(/lobby-client/);

    // wait for client to appear in player list then start
    await expect(hostPage.getByText('linguin0')).toBeVisible();

    // host starts game
    await hostPage.getByRole('button', { name: 'Start Quiz' }).click();

    // wait for client to reach question page
    await clientPage.waitForURL(/game/, { timeout: 60000 });
    await expect(clientPage.locator('.question-box')).toBeVisible();

    // wait for answer buttons to be populated w/ websocket
    await clientPage.waitForFunction(() => {
        const btn = document.querySelector('.answer-btn');
        return btn && btn.textContent.trim() !== '';
    });

    // client clicks answer
    await clientPage.locator('.answer-btn').first().click();
    await clientPage.waitForURL(/selected-answer/);
    await expect(clientPage.locator('.answer-box')).toBeVisible();

    // host reveals correct answer
    await hostPage.getByRole('button', { name: 'Next' }).click();
    await hostPage.waitForURL(/show-correct-answer/);

    // client should be redirected to correct ans page
    await clientPage.waitForURL(/show-correct-answer-client/);

    // verify host & client see correct answer box
    await expect(hostPage.locator('.answer-box')).toBeVisible();
    await expect(clientPage.locator('.answer-box')).toBeVisible();

    // host advances to leaderboard
    await hostPage.getByRole('button', { name: 'Next' }).click();
    await hostPage.waitForURL(/show-positions/);

    // client should be redirected to position page
    await clientPage.waitForURL(/client-position/);

    // verify client sees position and score
    await expect(clientPage.locator('.position')).toBeVisible();
    await expect(clientPage.locator('.score')).toBeVisible();

    // host clicks continue on leaderboard page
    await hostPage.getByRole('button', { name: 'Continue' }).click();

    // client should be redirected to end game screen
    await clientPage.waitForURL(/end-game/);

    // verify client sees final position and score
    await expect(clientPage.locator('.position')).toBeVisible();
    await expect(clientPage.locator('.score')).toBeVisible();

    await hostContext.close();
    await clientContext.close();
});

// testing new quit buttons on client-side
test('Client quit button redirects to student dashboard', async ({ browser }) => {
    const hostContext = await browser.newContext();
    const clientContext = await browser.newContext({ storageState: { cookies: [], origins: [] } });
    const hostPage = await hostContext.newPage();
    const clientPage = await clientContext.newPage();

    // host login & lobby creation
    await hostPage.goto('/login');
    await hostPage.getByLabel('Username:').fill('JoanaKennah');
    await hostPage.getByLabel('Password:').fill('password456');
    await hostPage.getByRole('button', { name: 'Enter' }).click();
    await hostPage.waitForURL(/teacher-dashboard/);
    await hostPage.goto('/view-quizzes');
    await hostPage.getByText('COMP2870 Quiz').click();
    await hostPage.getByRole('button', { name: 'Create a Lobby' }).click();
    await hostPage.getByRole('button', { name: 'Create Lobby!' }).click();
    await hostPage.waitForURL(/lobby-server/);

    // get room code
    const roomCodeText = await hostPage.getByText(/Room Code:/).textContent();
    const roomCode = roomCodeText.replace('Room Code: ', '').trim();

    // client login & join
    await clientPage.goto('/login');
    await clientPage.getByLabel('Username:').click();
    await clientPage.getByLabel('Username:').pressSequentially('linguin0');
    await clientPage.getByLabel('Password:', { exact: true }).click();
    await clientPage.getByLabel('Password:', { exact: true }).pressSequentially('password123');
    await clientPage.getByRole('button', { name: 'Enter' }).click();
    await clientPage.goto('/join-lobby');
    await clientPage.waitForLoadState('networkidle');
    await clientPage.getByLabel('Room Code').fill(roomCode);
    await clientPage.getByRole('button', { name: 'Join' }).click();
    await expect(clientPage).toHaveURL(/lobby-client/);

    // client clicks quit on navbar
    await clientPage.getByRole('link', { name: 'Quit' }).click();

    // client should be redirected to student dashboard (in this case)
    await clientPage.waitForURL(/student-dashboard/);

    await hostContext.close();
    await clientContext.close();
});

test('Host can kick client mid-game', async ({ browser }) => {
    const hostContext = await browser.newContext();
    const clientContext = await browser.newContext({ storageState: { cookies: [], origins: [] } });
    const hostPage = await hostContext.newPage();
    const clientPage = await clientContext.newPage();

    // host login & lobby creation
    await hostPage.goto('/login');
    await hostPage.getByLabel('Username:').fill('JoanaKennah');
    await hostPage.getByLabel('Password:').fill('password456');
    await hostPage.getByRole('button', { name: 'Enter' }).click();
    await hostPage.waitForURL(/teacher-dashboard/);
    await hostPage.goto('/view-quizzes');
    await hostPage.getByText('COMP2870 Quiz').click();
    await hostPage.getByRole('button', { name: 'Create a Lobby' }).click();
    await hostPage.getByRole('button', { name: 'Create Lobby!' }).click();
    await hostPage.waitForURL(/lobby-server/);

    // get room code
    const roomCodeText = await hostPage.getByText(/Room Code:/).textContent();
    const roomCode = roomCodeText.replace('Room Code: ', '').trim();

    // client login & join
    await clientPage.goto('/login');
    await clientPage.getByLabel('Username:').click();
    await clientPage.getByLabel('Username:').pressSequentially('linguin0');
    await clientPage.getByLabel('Password:', { exact: true }).click();
    await clientPage.getByLabel('Password:', { exact: true }).pressSequentially('password123');
    await clientPage.getByRole('button', { name: 'Enter' }).click();
    await clientPage.goto('/join-lobby');
    await clientPage.waitForLoadState('networkidle');
    await clientPage.getByLabel('Room Code').fill(roomCode);
    await clientPage.getByRole('button', { name: 'Join' }).click();
    await expect(clientPage).toHaveURL(/lobby-client/);

    // wait for client username to appear then start game
    await expect(hostPage.getByText('linguin0')).toBeVisible();
    await hostPage.getByRole('button', { name: 'Start Quiz' }).click();

    // wait for client to reach question page
    await clientPage.waitForURL(/game/, { timeout: 60000 });

    // wait for answer buttons to be populated via websocket
    await clientPage.waitForFunction(() => {
        const btn = document.querySelector('.answer-btn');
        return btn && btn.textContent.trim() !== '';
    });

    // client answers question
    await clientPage.locator('.answer-btn').first().click();
    await clientPage.waitForURL(/selected-answer/);

    // host advances to show correct answer
    await hostPage.getByRole('button', { name: 'Next' }).click();
    await hostPage.waitForURL(/show-correct-answer/);

    // host advances to leaderboard where Settings button is available
    await hostPage.getByRole('button', { name: 'Next' }).click();
    await hostPage.waitForURL(/show-positions/);

    // host navigates to manage participants and kicks client
    await hostPage.getByRole('button', { name: 'Settings' }).click();
    await hostPage.getByRole('button', { name: 'Manage Participants' }).click();
    await hostPage.getByText('linguin0').click();
    await hostPage.getByRole('button', { name: 'Kick Selected' }).click();
    await hostPage.getByRole('button', { name: 'Save' }).click();

    // client should be redirected to kicked page
    await clientPage.waitForURL(/kicked/, { timeout: 10000 });

    await hostContext.close();
    await clientContext.close();
});