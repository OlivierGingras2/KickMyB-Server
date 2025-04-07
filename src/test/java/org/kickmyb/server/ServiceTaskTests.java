package org.kickmyb.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kickmyb.server.account.MUser;
import org.kickmyb.server.account.MUserRepository;
import org.kickmyb.server.task.ServiceTask;
import org.kickmyb.transfer.AddTaskRequest;
import org.kickmyb.transfer.HomeItemResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// TODO pour celui ci on aimerait pouvoir mocker l'utilisateur pour ne pas avoir à le créer

// https://reflectoring.io/spring-boot-mock/#:~:text=This%20is%20easily%20done%20by,our%20controller%20can%20use%20it.

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = KickMyBServerApplication.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
//@ActiveProfiles("test")
class ServiceTaskTests {

    @Autowired
    private MUserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ServiceTask serviceTask;

    @Test
    void testDelete() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
        MUser u = new MUser();
        u.username = "M. Bean";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        MUser u2 = new MUser();
        u2.username = "Evil Schmungus";
        u2.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u2);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "À garder loin de Schmungus";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));
        serviceTask.addOne(atr, u);

        AddTaskRequest atr2 = new AddTaskRequest();
        atr2.name = "Plans malicieux de Schmungus";
        atr2.deadline = Date.from(new Date().toInstant().plusSeconds(3600));
        serviceTask.addOne(atr2, u2);

        List<HomeItemResponse> tasksBeforeDelete = serviceTask.home(u.id);
        assertEquals(1, tasksBeforeDelete.size());

        u = serviceTask.userFromUsername(u.username);
        u2 = serviceTask.userFromUsername(u2.username);

        HomeItemResponse taskToDelete = serviceTask.home(u.id).get(0);
        serviceTask.delete(taskToDelete.id, u2);

        List<HomeItemResponse> tasksAfterDelete = serviceTask.home(u.id);
        assertEquals(1, tasksAfterDelete.size());

        serviceTask.delete(taskToDelete.id, u);

        tasksAfterDelete = serviceTask.home(u.id);
        assertEquals(0, tasksAfterDelete.size());
    }



    @Test
    void testAddTask() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "Tâche de test";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        serviceTask.addOne(atr, u);

        assertEquals(1, serviceTask.home(u.id).size());
    }

    @Test
    void testAddTaskEmpty()  {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        try{
            serviceTask.addOne(atr, u);
            fail("Aurait du lancer ServiceTask.Empty");
        } catch (Exception e) {
            assertEquals(ServiceTask.Empty.class, e.getClass());
        }
    }

    @Test
    void testAddTaskTooShort() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "o";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        try{
            serviceTask.addOne(atr, u);
            fail("Aurait du lancer ServiceTask.TooShort");
        } catch (Exception e) {
            assertEquals(ServiceTask.TooShort.class, e.getClass());
        }
    }

    @Test
    void testAddTaskExisting() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "Bonne tâche";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        try{
            serviceTask.addOne(atr, u);
            serviceTask.addOne(atr, u);
            fail("Aurait du lancer ServiceTask.Existing");
        } catch (Exception e) {
            assertEquals(ServiceTask.Existing.class, e.getClass());
        }
    }
}
