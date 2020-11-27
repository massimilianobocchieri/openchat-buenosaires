package bsas.org.openchat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class OpenChatSystemTest {

    private OpenChatSystem system;
    private TestObjectsBucket testObjects = new TestObjectsBucket();

    @Test
    public void createSystemHasNoUsers() {
        //No lo inicializo en el setup por si quiero hacer restart del
        //contexto mientras debuggeo
        system = createSystem();

        assertFalse(system.hasUsers());
        assertFalse(system.hasUserNamed(TestObjectsBucket.PEPE_SANCHEZ_NAME));
        assertEquals(0, system.numberOfUsers());
    }
    @Test
    public void canRegisterUser() {
        system = createSystem();
        User registeredUser = registerPepeSanchez();

        assertTrue(system.hasUsers());
        assertTrue(system.hasUserNamed(TestObjectsBucket.PEPE_SANCHEZ_NAME));
        assertEquals(1,system.numberOfUsers());

        assertTrue(registeredUser.isNamed(TestObjectsBucket.PEPE_SANCHEZ_NAME));
        assertEquals(TestObjectsBucket.PEPE_SANCHEZ_ABOUT,registeredUser.about());
        assertEquals(TestObjectsBucket.PEPE_SANCHEZ_HOME_PAGE,registeredUser.homePage());
        assertNotEquals(TestObjectsBucket.PEPE_SANCHEZ_HOME_PAGE +"x",registeredUser.homePage());
    }
    @Test
    public void canRegisterManyUsers() {
        system = createSystem();
        registerPepeSanchez();
        registerJuanPerez();

        assertTrue(system.hasUsers());
        assertTrue(system.hasUserNamed(TestObjectsBucket.PEPE_SANCHEZ_NAME));
        assertTrue(system.hasUserNamed(TestObjectsBucket.JUAN_PEREZ_NAME));
        assertEquals(2,system.numberOfUsers());
    }
    @Test
    public void canNotRegisterSameUserTwice() {
        system = createSystem();
        registerPepeSanchez();

        TestObjectsBucket.assertThrowsModelExceptionWithErrorMessage(
                ()->registerPepeSanchez(),
                OpenChatSystem.CANNOT_REGISTER_SAME_USER_TWICE);

        assertTrue(system.hasUsers());
        assertTrue(system.hasUserNamed(TestObjectsBucket.PEPE_SANCHEZ_NAME));
        assertEquals(1,system.numberOfUsers());
    }
    @Test
    public void canWorkWithAuthenticatedUser() {
        system = createSystem();
        final User registeredUser = registerPepeSanchez();

        final Object token = new Object();
        final Object authenticatedToken = system.withAuthenticatedUserDo(
                TestObjectsBucket.PEPE_SANCHEZ_NAME,
                TestObjectsBucket.PEPE_SANCHEZ_PASSWORD,
                user->token,
                ()->fail());

        assertEquals(token,authenticatedToken);
    }
    @Test
    public void notRegisteredUserIsNotAuthenticated() {
        system = createSystem();
        assertCanNotAuthenticatePepeSanchezWith(TestObjectsBucket.PEPE_SANCHEZ_PASSWORD);
    }
    @Test
    public void canNotAuthenticateWithInvalidPassword() {
        system = createSystem();
        registerPepeSanchez();

        assertCanNotAuthenticatePepeSanchezWith(TestObjectsBucket.PEPE_SANCHEZ_PASSWORD+"something");
    }
    @Test
    public void registeredUserCanPublish() {
        system = createSystem();
        final User registeredUser = registerPepeSanchez();

        Publication publication = system.publishForUserIdentifiedAs(registeredUser.id(),"hello");

        List<Publication> timeLine = system.timeLineOfUserIdentifiedAs(registeredUser.id());
        assertEquals(Arrays.asList(publication),timeLine);
    }
    @Test
    public void noRegisteredUserCanNotPublish() {
        system = createSystem();

        TestObjectsBucket.assertThrowsModelExceptionWithErrorMessage(
                ()->system.publishForUserIdentifiedAs(UUID.randomUUID().toString(),"hello"),
                OpenChatSystem.USER_NOT_REGISTERED);
    }
    @Test
    public void noRegisteredUserCanAskItsTimeline() {
        system = createSystem();

        TestObjectsBucket.assertThrowsModelExceptionWithErrorMessage(
                ()->system.timeLineOfUserIdentifiedAs(UUID.randomUUID().toString()),
                OpenChatSystem.USER_NOT_REGISTERED);
    }
    @Test
    public void canFollowRegisteredUser() {
        system = createSystem();
        final User followed = registerPepeSanchez();
        final User follower = registerJuanPerez();

        system.followedByFollowerIdentifiedAs(followed.id(), follower.id());

        List<User> followers = system.followersOfUserIdentifiedAs(followed.id());
        assertEquals(Arrays.asList(follower),followers);
    }
    @Test
    public void canGetWallOfRegisteredUser() {
        system = createSystem();
        final User followed = registerPepeSanchez();
        final User follower = registerJuanPerez();
        system.followedByFollowerIdentifiedAs(followed.id(), follower.id());

        Publication followedPublication = system.publishForUserIdentifiedAs(followed.id(),"hello");
        testObjects.changeNowTo(testObjects.now().plusSeconds(1));
        Publication followerPublication = system.publishForUserIdentifiedAs(follower.id(),"bye");

        List<Publication> wall = system.wallOfUserIdentifiedAs(followed.id());
        assertEquals(Arrays.asList(followerPublication,followedPublication),wall);
    }
    @Test
    public void publicationsHaveNoLikesWhenCreated() {
        system = createSystem();
        final User registeredUser = registerPepeSanchez();

        Publication publication = system.publishForUserIdentifiedAs(registeredUser.id(),"hello");
        assertEquals(0,system.likesOf(publication));
    }
    @Test
    public void registeredUserCanLikePublication() {
        system = createSystem();
        final User publisher = registerPepeSanchez();
        final User liker = registerJuanPerez();

        Publication publication = system.publishForUserIdentifiedAs(publisher.id(),"hello");
        int likes = system.likePublication(publication,liker.id());

        assertEquals(1,likes);
        assertEquals(1,system.likesOf(publication));
    }
    @Test
    public void canNotLikeNotPublishPublication() {
        system = createSystem();
        User registeredUser = registerPepeSanchez();

        Publication publication = Publication.madeBy(Publisher.relatedTo(registeredUser),"hello", testObjects.now());
        TestObjectsBucket.assertThrowsModelExceptionWithErrorMessage(
                ()->system.likePublication(publication,registeredUser.id()),
                OpenChatSystem.INVALID_PUBLICATION);
    }
    @Test
    public void likesByUserCountOnlyOnce() {
        system = createSystem();
        final User publisher = registerPepeSanchez();
        final User liker = registerJuanPerez();

        Publication publication = system.publishForUserIdentifiedAs(publisher.id(),"hello");
        system.likePublication(publication,liker.id());
        int likes = system.likePublication(publication,liker.id());

        assertEquals(1,likes);
        assertEquals(1,system.likesOf(publication));
    }
    @Test
    public void notRegisteredUserCanNotLikePublication() {
        system = createSystem();
        final User publisher = registerPepeSanchez();

        Publication publication = system.publishForUserIdentifiedAs(publisher.id(),"hello");
        TestObjectsBucket.assertThrowsModelExceptionWithErrorMessage(
                ()->system.likePublication(publication,UUID.randomUUID().toString()),
                OpenChatSystem.USER_NOT_REGISTERED);
    }

    private void assertCanNotAuthenticatePepeSanchezWith(String password) {
        final Object token = new Object();
        final Object notAuthenticatedToken = system.withAuthenticatedUserDo(
                TestObjectsBucket.PEPE_SANCHEZ_NAME,
                password,
                user->fail(),
                ()-> token);

        assertEquals(token,notAuthenticatedToken);
    }

    private OpenChatSystem createSystem() {
        return new OpenChatSystem(testObjects.fixedNowClock());
    }

    private User registerPepeSanchez() {
        return system.register(
                TestObjectsBucket.PEPE_SANCHEZ_NAME,
                TestObjectsBucket.PEPE_SANCHEZ_PASSWORD,
                TestObjectsBucket.PEPE_SANCHEZ_ABOUT,
                TestObjectsBucket.PEPE_SANCHEZ_HOME_PAGE);
    }

    private User registerJuanPerez() {
        return system.register(
                TestObjectsBucket.JUAN_PEREZ_NAME,
                TestObjectsBucket.JUAN_PEREZ_PASSWORD,
                TestObjectsBucket.JUAN_PEREZ_ABOUT,
                TestObjectsBucket.JUAN_PEREZ_HOME_PAGE);
    }
}