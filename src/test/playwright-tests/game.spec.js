// @ts-check
import { test, expect } from '@playwright/test';

// Note for future testing ---
// These tests need to be run with --workers=1 because sharing server state breaks them.
// This is purely test architecture and does not affect a real scenario

test.setTimeout(120000); // 2 minutes to allow for game flow

test('Full game flow from start to end screen', async ({ browser }) => {
    const hostContext = await browser.newContext();
    const clientContext = await browser.newContext({ storageState: { cookies: [], origins: [] } });
    const hostPage = await hostContext.newPage();
    const clientPage = await clientContext.newPage();

    // host login and lobby creation
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

    // client login and join
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

    // wait for client to appear in player list before starting
    await expect(hostPage.getByText('linguin0')).toBeVisible();

    // host starts game
    await hostPage.getByRole('button', { name: 'Start Quiz' }).click();

    // wait for client to reach question page
    await clientPage.waitForURL(/game/, { timeout: 60000 });
    await expect(clientPage.locator('.question-box')).toBeVisible();

    // wait for answer buttons to be populated via websocket
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

    // client should be redirected to correct answer page
    await clientPage.waitForURL(/show-correct-answer-client/);

    // verify both see the correct answer box
    await expect(hostPage.locator('.answer-box')).toBeVisible();
    await expect(clientPage.locator('.answer-box')).toBeVisible();

    // host advances to leaderboard
    await hostPage.getByRole('button', { name: 'Next' }).click();
    await hostPage.waitForURL(/show-positions/);

    // client should be redirected to their position page
    await clientPage.waitForURL(/client-position/);

    // verify client sees their position and score
    await expect(clientPage.locator('.position')).toBeVisible();
    await expect(clientPage.locator('.score')).toBeVisible();

    // host clicks continue on leaderboard
    await hostPage.getByRole('button', { name: 'Continue' }).click();

    // client should be redirected to end game screen
    await clientPage.waitForURL(/end-game/);

    // verify client sees their final position and score
    await expect(clientPage.locator('.position')).toBeVisible();
    await expect(clientPage.locator('.score')).toBeVisible();

    await hostContext.close();
    await clientContext.close();
});