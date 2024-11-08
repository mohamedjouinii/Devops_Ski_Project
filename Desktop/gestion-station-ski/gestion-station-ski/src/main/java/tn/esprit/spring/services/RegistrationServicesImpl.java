package tn.esprit.spring.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.spring.entities.*;
import tn.esprit.spring.repositories.ICourseRepository;
import tn.esprit.spring.repositories.IRegistrationRepository;
import tn.esprit.spring.repositories.ISkierRepository;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public class RegistrationServicesImpl implements IRegistrationServices {

    private final IRegistrationRepository registrationRepository;
    private final ISkierRepository skierRepository;
    private final ICourseRepository courseRepository;

    @Override
    public Registration addRegistrationAndAssignToSkier(Registration registration, Long numSkier) {
        log.info("Attempting to add registration for skier with ID: {}", numSkier);

        Skier skier = skierRepository.findById(numSkier).orElse(null);
        if (skier == null) {
            log.error("Skier with ID {} not found", numSkier);
            return null;
        }

        registration.setSkier(skier);
        Registration savedRegistration = registrationRepository.save(registration);
        log.info("Registration successfully saved for skier ID: {}", numSkier);
        return savedRegistration;
    }

    @Override
    public Registration assignRegistrationToCourse(Long numRegistration, Long numCourse) {
        log.info("Assigning registration with ID {} to course with ID {}", numRegistration, numCourse);

        Registration registration = registrationRepository.findById(numRegistration).orElse(null);
        Course course = courseRepository.findById(numCourse).orElse(null);

        if (registration == null || course == null) {
            log.error("Either registration ID {} or course ID {} not found", numRegistration, numCourse);
            return null;
        }

        registration.setCourse(course);
        Registration updatedRegistration = registrationRepository.save(registration);
        log.info("Registration ID {} successfully assigned to course ID {}", numRegistration, numCourse);
        return updatedRegistration;
    }

    @Transactional
    @Override
    public Registration addRegistrationAndAssignToSkierAndCourse(Registration registration, Long numSkieur, Long numCours) {
        log.info("Adding registration and assigning to skier ID {} and course ID {}", numSkieur, numCours);

        Skier skier = skierRepository.findById(numSkieur).orElse(null);
        Course course = courseRepository.findById(numCours).orElse(null);

        if (skier == null) {
            log.error("Skier with ID {} not found", numSkieur);
            return null;
        }
        if (course == null) {
            log.error("Course with ID {} not found", numCours);
            return null;
        }

        // Vérifier si l'inscription existe déjà
        boolean alreadyRegistered = registrationRepository.countDistinctByNumWeekAndSkier_NumSkierAndCourse_NumCourse(
                registration.getNumWeek(), skier.getNumSkier(), course.getNumCourse()) >= 1;

        if (alreadyRegistered) {
            log.warn("Skier ID {} is already registered for course ID {} in week {}", skier.getNumSkier(), course.getNumCourse(), registration.getNumWeek());
            return null;
        }

        int ageSkieur = Period.between(skier.getDateOfBirth(), LocalDate.now()).getYears();
        log.info("Skier age: {}", ageSkieur);

        // Vérifier le type de cours et les contraintes d'âge
        switch (course.getTypeCourse()) {
            case INDIVIDUAL:
                log.info("Assigning to individual course");
                return assignRegistration(registration, skier, course);

            case COLLECTIVE_CHILDREN:
                if (ageSkieur < 16) {
                    log.info("Eligible for children's collective course");
                    if (registrationRepository.countByCourseAndNumWeek(course, registration.getNumWeek()) < 6) {
                        log.info("Seats available for children's course");
                        return assignRegistration(registration, skier, course);
                    } else {
                        log.warn("Children's course is full for week {}", registration.getNumWeek());
                        return null;
                    }
                } else {
                    log.warn("Skier age {} is not eligible for children's course", ageSkieur);
                    return null;
                }

            case COLLECTIVE_ADULT:
                if (ageSkieur >= 16) {
                    log.info("Eligible for adult collective course");
                    if (registrationRepository.countByCourseAndNumWeek(course, registration.getNumWeek()) < 6) {
                        log.info("Seats available for adult course");
                        return assignRegistration(registration, skier, course);
                    } else {
                        log.warn("Adult course is full for week {}", registration.getNumWeek());
                        return null;
                    }
                } else {
                    log.warn("Skier age {} is not eligible for adult course", ageSkieur);
                    return null;
                }

            default:
                log.error("Unknown course type for course ID {}", course.getNumCourse());
                return null;
        }
    }

    private Registration assignRegistration(Registration registration, Skier skier, Course course) {
        log.debug("Assigning registration to skier ID {} and course ID {}", skier.getNumSkier(), course.getNumCourse());

        registration.setSkier(skier);
        registration.setCourse(course);

        Registration savedRegistration = registrationRepository.save(registration);
        log.info("Registration saved successfully for skier ID {} and course ID {}", skier.getNumSkier(), course.getNumCourse());
        return savedRegistration;
    }

    @Override
    public List<Integer> numWeeksCourseOfInstructorBySupport(Long numInstructor, Support support) {
        log.info("Fetching weeks for instructor ID {} with support {}", numInstructor, support);

        List<Integer> weeks = registrationRepository.numWeeksCourseOfInstructorBySupport(numInstructor, support);
        log.debug("Weeks found: {}", weeks);
        return weeks;
    }
}
