import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Main {
	static LocalDate rawDateToday = LocalDate.now();
	static String dateToday = String.format("%02d/%02d/%d", rawDateToday.getMonthValue(), rawDateToday.getDayOfMonth(), rawDateToday.getYear());
	static String monthYearToday = String.format("%02d/%d", rawDateToday.getMonthValue(), rawDateToday.getYear());
	static String fileName = null;
	static String folderName = "acc";
	static Scanner input = new Scanner(System.in);
	
	public static void main(String[] args) {
		String credsFileName = "credentials.txt";
		try {new File(credsFileName).createNewFile(); new File(folderName).mkdir();} catch (IOException e) {e.printStackTrace();}
		boolean lockedIn = false;
		String username = null;
		
		// Initial implementation of user authentication
		// Not recommended, as it involves various security issues such as brute force, data breach, ...
		String authenticationPrompt = """

(1) Sign in
(2) Create account
				""";
		
		while (!lockedIn) {
			try {
				System.out.println(authenticationPrompt);
				System.out.print("> ");
				int authOptionSelected = input.nextInt();
				input.nextLine();
				
				switch (authOptionSelected) {
				case 1:
					username = textValidation("Username", "Username must not be blank");
					String password = textValidation("Password", "Password must not be blank");
					String usernameAndPassword = username + "~" + password;
					try (BufferedReader reader = new BufferedReader(new FileReader(credsFileName))) {
						String line;
						
						while ((line = reader.readLine()) != null) {
							if (line.equals(usernameAndPassword)) {
								lockedIn = true;
								fileName = String.format("%s/%s.txt", folderName, username);
								break;
							}
						}
						if (!lockedIn) {
							System.out.println(withColor("Username/Password combination incorrect.", 93));
							username = null;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;
				case 2:
					while (username == null) {
						username = textValidation("Username", "Username must not be blank", "\\/:*~\"`?<>|");
						try (BufferedReader reader = new BufferedReader(new FileReader(credsFileName))) {
							String line;
							
							while ((line = reader.readLine()) != null) {
								String[] info = line.split("~");
								if (info[0].equals(username)) {
									username = null;
									System.out.println(withColor("Username already in use. Please choose a different one.\n", 93));
									break;
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					
					String newUserPassword = textValidation("Password", "Password must not be blank");
					
					try (BufferedWriter writer = new BufferedWriter(new FileWriter(credsFileName, true))) {
						writer.append(username + "~" + newUserPassword);
						writer.newLine();
						System.out.println(withColor("Your account was successfully created.\n", 36));
						System.out.printf("Here's the summary of your credentials:\nUsername: %s\nPassword: %s\n\n", username, newUserPassword);
						fileName = String.format("%s/%s.txt", folderName, username);
						lockedIn = true;
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;
				default: System.out.println(withColor("Kindly choose between option 1 and 2.", 93));
				}
			} catch (InputMismatchException e) {
				System.out.println(withColor("Kindly choose between option 1 and 2.", 93));
				input.nextLine();
			}
		}
		
		// HOME PAGE
		try {new File(fileName).createNewFile();} catch (IOException e) {e.printStackTrace();}
		System.out.printf("\nCalman -- " + withColor("Greetings, %s. Here is our menu.\n", 92), username);
		String menu = """

Menu:
(1) Create New Schedule
(2) Modify Existing Schedule
(3) Remove Schedule
(4) View Monthly Calendar
(5) View Daily Schedule/s
(6) Search for Schedule
(7) Exit
				""";
		
		while (lockedIn) {
			try {
				System.out.println(menu);
				System.out.print("> ");
				int selectedOption = input.nextInt();
				input.nextLine();
				
				switch (selectedOption) {
				case 1: addOption(); break;
				case 2: modifyOption(); break;
				case 3: deleteOption(); break;
				case 4: viewCalendarOption(); break;
				case 5: viewDailyOption(); break;
				case 6: searchOption(); break;
				case 7: lockedIn = exitOption(); break;
				default: System.out.println(withColor("Kindly choose the option listed on the menu.", 93));
				}
			} catch (InputMismatchException e) {
				System.out.println(withColor("Kindly choose the option listed on the menu.", 93));
				input.nextLine();
			}
		}
	}
	
	
	// MAIN OPTIONS METHOD	
	public static void addOption() {
		System.out.println("New Schedule*\n");
		String title = textValidation("Title", "Title must not be blank", "~`");
		System.out.println();
		
		String date = dateValidation("Date");
		
		List<String> schedules = new ArrayList<>();
		fetchDailySchedules(schedules, date);
		
		if (!schedules.isEmpty()) {
			System.out.print("Kindly review your schedule for available time slots.");
			displayDailySchedules(schedules, date);
			System.out.println("\n");
		}
		date = recurringSchedule(date);
		
		String startTime = startTimeValidation("Start time", schedules);
		int startTimeValue = militaryTime(startTime);
		
		String endTime = endTimeValidation("End time", startTime, startTimeValue, schedules);
		int endTimeValue = militaryTime(endTime);
		
		String values = String.format("%s~%s~%s~%s~%s~%s", date, title, startTime, endTime, startTimeValue, endTimeValue);
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
			writer.append(values);
			writer.newLine();
			System.out.println(withColor("Schedule added successfully.", 36));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void modifyOption() {
		List<String> matchedSchedules = new ArrayList<>();
		List<String> schedules = new ArrayList<>();
		
		System.out.println("Modify Schedule*\n");
		String keyword = textValidation("Input keyword/s", "Keyword/s required");
		
		fetchSchedulesByTitle(matchedSchedules, keyword);
		
		if (!matchedSchedules.isEmpty()) {
			displayMatchedSchedules(matchedSchedules);
			while (true) {
				try {
					System.out.print("\nEnter the schedule's index to modify: ");
					int scheduleIndex = input.nextInt();
					input.nextLine();
					String toBeModifedValue = matchedSchedules.get(scheduleIndex);
					String[] oldValueParts = toBeModifedValue.split("~");
					
					System.out.println("\nPreviuos title \"" + withColor(oldValueParts[1], 36) + "\"");
					String title = textValidation("New Title", "Title must not be blank");
					System.out.println();

					System.out.println("Previuos date (" + withColor(oldValueParts[0], 36) + ")");
					String date = dateValidation("New date");
					fetchDailySchedules(schedules, date);
					schedules.remove(toBeModifedValue);
					
					date = recurringSchedule(date);
					
					System.out.println("Previuos start time (" + withColor(oldValueParts[2], 36) + ")");
					String startTime = startTimeValidation("New start time", schedules);
					int startTimeValue = militaryTime(startTime);
					
					System.out.println("Previuos end time (" + withColor(oldValueParts[3], 36) + ")");
					String endTime = endTimeValidation("New end time", startTime, startTimeValue, schedules);
					int endTimeValue = militaryTime(endTime);
					
					String newValue = String.format("%s~%s~%s~%s~%s~%s", date, title, startTime, endTime, startTimeValue, endTimeValue);

					modifyLine(toBeModifedValue, newValue);
					System.out.printf(withColor("Modified Successfully.\n", 36));
					break;
				} catch (InputMismatchException e) {
					System.out.println(withColor("Invalid index.", 91));
					input.nextLine();
				} catch (IndexOutOfBoundsException e) {
					System.out.println(withColor("Invalid index.", 91));
				}
			}
		} else {
			System.out.println(withColor("No match found.", 93));
		}
	}
	
	public static void deleteOption() {
		List<String> schedules = new ArrayList<>();
		
		System.out.println("Delete Schedule*\n");
		String keyword = textValidation("Input keyword/s", "Keyword/s required");
		
		fetchSchedulesByTitle(schedules, keyword);
		
		if (!schedules.isEmpty()) {
			displayMatchedSchedules(schedules);
			while (true) {
				try {
					System.out.print("\nEnter the schedule's index to delete: ");
					int scheduleIndex = input.nextInt();
					input.nextLine();
					System.out.println();
					String toBeDeletedValue = schedules.get(scheduleIndex);
					boolean proceedToDelete = deleteConfirmation(scheduleIndex);
					
					if (proceedToDelete) {
						deleteLine(toBeDeletedValue);
						System.out.printf(withColor("Deleted Successfully.\n", 36));
					} else {						
						System.out.printf(withColor("Deletion cancelled.\n", 93));
					}
					break;
				} catch (InputMismatchException e) {
					System.out.println(withColor("Invalid index.", 91));
					input.nextLine();
				} catch (IndexOutOfBoundsException e) {
					System.out.println(withColor("Invalid index.", 91));
				}
			}
		} else {
			System.out.println(withColor("No match found.", 93));
		}
	}
	
	public static void viewCalendarOption() {
		HashSet<Integer> scheduleDates = new HashSet<>();
		HashSet<Integer> specialDates = new HashSet<>();
		
		fetchMonthlySchedules(scheduleDates, specialDates, intToStr(rawDateToday.getMonthValue()), intToStr(rawDateToday.getYear()));
		displayMonthlySchedules(scheduleDates, specialDates, monthYearToday, intToStr(rawDateToday.getMonthValue()), intToStr(rawDateToday.getYear()));
		
		System.out.println("\nPress enter to go back");
		input.nextLine();
	}
	
	public static void viewDailyOption() {
		
		List<String> schedules = new ArrayList<>();
		fetchDailySchedules(schedules, dateToday);
		
		if (!schedules.isEmpty()) {
			System.out.println("Today's Schedule/s");
			displayDailySchedules(schedules, dateToday);
			System.out.println("\nPress enter to go back");
			input.nextLine();
		} else {
			System.out.println(withColor("No events scheduled.", 93));
		}
	}
	
	public static void searchOption() {
		String searchMenu = """

Search Menu:
(1) By month
(2) By day
(3) By title
				""";
		while (true) {
			try {
				System.out.println(searchMenu);
				System.out.print("> ");
				int searchMenuOption = input.nextInt();
				input.nextLine();
				
				switch (searchMenuOption) {
				case 1: viewMonthlySchedules(); break;
				case 2: viewDailySchedules(); break;
				case 3: viewByTitle(); break;
				default:
					System.out.println(withColor("Kindly choose the option listed on the search menu.", 93));
					continue;
				}
				break;
			} catch (InputMismatchException e) {
				System.out.println(withColor("Kindly choose the option listed on the search menu.", 93));
				input.nextLine();
			}
		}
	}
	
	public static void viewMonthlySchedules() {
		String monthYear = monthYearValidation("Input month and year");

		String[] monthYearParts = monthYear.split("/");
		String strMonth = monthYearParts[0];
		String strYear = monthYearParts[1];
		HashSet<Integer> scheduleDates = new HashSet<>();
		HashSet<Integer> specialDates = new HashSet<>();
		
		fetchMonthlySchedules(scheduleDates, specialDates, strMonth, strYear);
		displayMonthlySchedules(scheduleDates, specialDates, monthYear, strMonth, strYear);
		
		System.out.println("\nPress enter to go back");
		input.nextLine();
	}
	
	public static void viewDailySchedules() {
		List<String> schedules = new ArrayList<>();
		
		String referenceDate = dateValidation("Input date");
		
		fetchDailySchedules(schedules, referenceDate);
		
		if (!schedules.isEmpty()) {
			displayDailySchedules(schedules, referenceDate);
			System.out.println("\nPress enter to go back");
			input.nextLine();
		} else {
			System.out.println(withColor("No events scheduled.", 93));
		}
	}
	
	public static void viewByTitle() {
		List<String> schedules = new ArrayList<>();
		String keyword = textValidation("Input keyword/s", "Keyword/s required");
		
		fetchSchedulesByTitle(schedules, keyword);
		
		if (!schedules.isEmpty()) {
			displayMatchedSchedules(schedules);
		} else {
			System.out.println(withColor("No match found.", 93));
		}
		System.out.println("\nPress enter to go back");
		input.nextLine();
	}
	
	public static boolean exitOption() {
		String mewingTextArt = """
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⡀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣿⣿⣿⣾⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⠀⢂⠀⠀⠀⠀⠋⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣿⣿⣿⣿⠀
⠀⠀⠀⠀⠀⠀⢀⡈⠛⠦⠀⠀⠀⠆⠀⠀⠀⠀⡀⠀⠐⠀⠀⠀⠀⠀⠀⡛⢱⣿⣿⠀
⠀⠀⠀⠠⣤⣴⠝⢛⠂⠀⠀⣐⣤⣠⣤⣰⣤⣜⢀⠀⢀⠀⠀⠀⠀⠀⠀⡅⣸⣿⣿⠀
⠀⠀⠀⠀⢖⣂⠄⡀⣤⣶⣿⣿⣿⣿⣿⣿⣿⣿⣷⣦⡀⠀⠁⠀⠀⠀⠀⣇⣿⣿⣿⠀
⠀⠀⠀⠀⣤⣶⣾⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⢿⣿⣿⣶⣦⠀⠀⠀⠀⢹⠹⣿⣿⠀
⠀⠀⠀⣤⢻⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⢟⣽⡪⠽⠛⡛⠻⣿⣷⠠⠀⠀⠀⠀⢻⣿⠀
⠀⠀⠀⢦⣼⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠟⠁⣠⣶⣾⣷⣤⡘⢟⡣⠀⠀⠀⠀⠘⣿⠀
⠀⠀⠀⡞⠿⢿⣿⣿⣿⣿⡿⣿⡿⠋⠀⣠⠾⣻⣽⠾⠻⣿⣿⡜⡷⡀⠀⠀⠀⢶⣿⠀
⠀⠀⠀⢠⣶⣤⣄⣀⠉⠻⢿⣶⡏⣡⡾⠗⣩⡀⣀⣶⣶⣾⣿⣿⡸⣧⠀⣢⠄⠸⣿⠀
⠀⠀⠀⠘⣿⠿⠛⠋⠋⠝⡃⣿⣿⣬⡻⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡄⢰⡹⣼⠀⢿⠀
⠀⠀⠀⠀⣯⡰⣗⣴⣾⣿⢡⢹⣿⣎⢷⣽⣿⣿⣿⣿⣿⣿⣿⣿⣿⡁⢸⣛⡟⠀⣾⠀
⠀⠀⠀⠀⣿⣿⣿⣿⣿⣿⡏⢸⣿⣿⣎⠿⣿⣿⣿⣿⣿⣿⣿⡿⢹⣿⠀⠉⠀⣴⣿⠀
⠀⠀⠀⠀⠸⣿⣿⣿⣿⣿⡟⣿⣿⣿⣿⣿⡌⣿⣿⣿⣿⣿⡟⢠⣿⣿⠀⠀⠀⠉⢸⠀
⠀⠀⠀⠀⠀⠹⣿⣿⣿⣿⣏⠌⠋⣩⡶⣒⣵⣿⣿⣿⣿⣿⣷⣿⣿⣿⠀⠀⠀⠀⢸⠀
⠀⠀⠀⠀⠀⠀⠈⠙⢛⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠟⣻⣿⣿⣿⣿⡟⡄⢀⠀⠀⠈⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠐⢝⢿⣿⡛⢯⡶⢶⣒⣛⣧⣾⢿⣿⣿⣿⡿⢡⣿⠈⢧⡀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠈⡐⣶⣾⣿⣿⣿⠿⣻⣻⣿⣿⣿⡿⢡⣿⣿⡇⠆⢧⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢨⣭⣭⣥⣶⣾⣿⣿⣿⣿⡟⣡⣿⣿⣿⣿⢰⢸⡖⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⢊⡾⡁⠻⣿⣿⣿⣿⣿⣿⣿⠏⣰⣿⣿⣿⣿⣿⣼⡟⣿⠀
⠀⠀⠀⠀⠀⠀⠀⡠⣠⢳⣧⡺⠁⡄⣝⡻⠿⠿⠿⢛⣡⣾⣿⣿⣿⣿⣿⣿⢻⣧⣿⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
	""";
		System.out.println(withColor("bye bye >,<", 92));
		System.out.println(mewingTextArt);
		input.close();
		return false;
	}
	
	
	// UTILITY METHODS	
	public static String withColor(String text, int foregroundColorCode) {
		return String.format("\u001B[%dm%s\u001B[0m", foregroundColorCode, text);
	}
	
	public static String withColor2(String text, int foregroundColorCodeBefore, int foregroundColorCodeAfter) {
		return String.format("\u001B[%dm%s\u001B[%dm", foregroundColorCodeBefore, text, foregroundColorCodeAfter);
	}
	
	public static int strToInt(String number) {
		return Integer.parseInt(number);
	}
	
	public static String intToStr(int number) {
		return Integer.toString(number);
	}
	
	public static String getMonthName(int month) {
		String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
		
		try {
			return monthNames[month-1];
		} catch (ArrayIndexOutOfBoundsException e) {
			return "Invalid Month";
		}
	}
	
	public static int daysOfTheMonth(int month, int year) {
		String yearType = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0) ? "Leap" : "Usual";
		int[] daysOfMonthsList = {31, yearType == "Leap" ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
		return daysOfMonthsList[month-1];
	}
	
	public static int determineDayOfTheWeek(int month, int day, int year) {
		// prepare values required in the formula below
        if (month == 1 || month == 2) {
        	month += 12;
        	year -= 1;
        }
        
        int century = year / 100;
        int centuryYear = year % 100;
        
        // formula for Zeller's Congruence Theorem Modified Version
        int h = (day + 
        		(26 * (month + 1) / 10) + 
        		centuryYear + 
        		(centuryYear / 4) + 
        		(century / 4) + 
        		5 * century) % 7;

		if (h >= 2 && h <= 6) {
			h -= 1;
		}
		else {
			h += 6;
		}
		return h;
	}
	
	public static String formatDate(String date) {
		String[] parts = date.split("/");
		return String.format("%02d/%02d/%s", strToInt(parts[0]), strToInt(parts[1]), parts[2]);
	}
	
	public static String formatMonthYear(String monthYear) {
		String[] parts = monthYear.split("/");
		return String.format("%02d/%s", strToInt(parts[0]), parts[1]);
	}
	
	public static String formatTime(String time) {
		String[] parts = time.split(":");
		String[] minuteAndMeridiem = parts[1].split("(?<=\\d)(?=\\D)");
		return String.format("%02d:%02d%s", Integer.parseInt(parts[0]), Integer.parseInt(minuteAndMeridiem[0]), minuteAndMeridiem[1]);
	}
	
	public static int militaryTime(String time) {
		String[] timeParts = time.split(":");
		String[] minuteAndMeridiem = timeParts[1].split("(?<=\\d)(?=\\D)");
		int hour = strToInt(timeParts[0]);
		int minute = strToInt(minuteAndMeridiem[0]);
		String meridiem = minuteAndMeridiem[1];

		if (meridiem.equalsIgnoreCase("pm") && hour != 12) {
			hour += 12;
		} 
		else if (meridiem.equalsIgnoreCase("am") && hour == 12) {
			hour = 0;
		}
		return hour * 60 + minute;
	}
	
	public static void fetchDailySchedules(List<String> schedules, String date) {
		String line;
		
		try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
			while ((line = reader.readLine()) != null) {
				String[] info = line.split("~");
				String curLineDate = info[0];
				boolean isDateEqual = curLineDate.equals(date) || (curLineDate.length() == 5 && curLineDate.equals(date.substring(0, 5)));
				
				// Schedules list has value && date is matched even if it is a recurring schedule
				if (schedules.isEmpty() && isDateEqual) {
					schedules.add(line);
				} 
				else if (isDateEqual) {
					int timeValue = strToInt(info[4]);
					int currentTimeValue = 0;
					boolean isAdded = false;

					for (int i = 0; i < schedules.size(); i++) {
						currentTimeValue = strToInt(schedules.get(i).split("~")[4]);
						if (currentTimeValue >= timeValue) {
							schedules.add(i, line);
							isAdded = true;
							break;
						}
					}
					// Add to the last 
					if (!isAdded) {
						schedules.add(line);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void fetchMonthlySchedules(HashSet<Integer> scheduleDates, HashSet<Integer> specialDates, String strMonth, String strYear) {
		try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] dateParts = line.split("~")[0].split("/");
				if (!strMonth.equals(dateParts[0])) continue;
				
				int day = strToInt(dateParts[1]);
				if (dateParts.length == 3 && strYear.equals(dateParts[2])) {
					scheduleDates.add(day);
				} else if (dateParts.length == 2) {
					specialDates.add(day);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void fetchSchedulesByTitle(List<String> schedules, String keyword) {
		try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
			String[] keywordChunks = keyword.trim().split("\\s+"); // match every word with symbol/s
			String line;
			
			while ((line = reader.readLine()) != null) {
				String[] currentSchedule = line.split("~");
				if (keywordChunks.length > 0) {
					for (String chunk : keywordChunks) {
						if (currentSchedule[1].toLowerCase().contains(chunk.toLowerCase())) {
							schedules.add(line);
							break;
						}
					}
				} else if (currentSchedule[1].toLowerCase().contains(keyword.toLowerCase())) {
					schedules.add(line);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void deleteLine(String toBeDeletedValue) {
		try (BufferedReader reader = new BufferedReader(new FileReader(fileName));
				BufferedWriter writer = new BufferedWriter(new FileWriter("tempFile.txt", true))) {
			
			String line;
			
			while ((line = reader.readLine()) != null) {
				if (!line.equals(toBeDeletedValue)) {
					writer.append(line);
					writer.newLine();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			new File(fileName).delete();
			File originalFile = new File(fileName);
			new File("tempFile.txt").renameTo(originalFile);
		}
	}
	
	public static void modifyLine(String toBeModifiedValue, String newValue) {
		try (BufferedReader reader = new BufferedReader(new FileReader(fileName));
				BufferedWriter writer = new BufferedWriter(new FileWriter("tempFile.txt", true))) {
			
			String line;
			
			while ((line = reader.readLine()) != null) {
				writer.append(line.equals(toBeModifiedValue) ? newValue : line);
				writer.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			new File(fileName).delete();
			File originalFile = new File(fileName);
			new File("tempFile.txt").renameTo(originalFile);
		}
	}
	
	
	// VALIDATION METHODS
	public static String textValidation(String prompt, String errorMessage) {
		String text = null;
		
		while (true) {
			System.out.printf("%s: ", prompt);
			text = input.nextLine();
			if (!text.isBlank()) {
				break;
			}
			System.out.println(withColor(errorMessage + ".\n", 91));
		}
		return text.trim();
	}
	
	public static String textValidation(String prompt, String errorMessage, String strConstraints) {
		String text = null;
		Pattern pattern = Pattern.compile("[" + strConstraints + "]");
		while (true) {
			System.out.printf("%s: ", prompt);
			text = input.nextLine();
			
			// ensures that the string is not blank and does not contain any of the constraints
			if (!text.isBlank() && !pattern.matcher(text).find()) {
				break;
			}
			System.out.println(withColor(String.format("%s and cannot contain any of these:\n%s\n", errorMessage, strConstraints), 91));
		}
		return text.trim();
	}
	
	public static boolean isDateValid(String date) { // date format: MM/DD/YYYY
		String datePattern = "(0?[1-9]|1[0-2])/(\\d{1,2})/([12][0-9][0-9][0-9])";
		boolean initialValidation = date.matches(datePattern);
		
		if (initialValidation) {
			String[] dateParts = date.split("/");
			int month = strToInt(dateParts[0]);
			int day = strToInt(dateParts[1]);
			int year = strToInt(dateParts[2]);
			int numOfDays = daysOfTheMonth(month, year);
			boolean finalValidation = day >= 1 && day <= numOfDays;

			if (!finalValidation) {
				String monthName = getMonthName(month);
				System.out.printf(withColor("There is no %s %d, %d.\n", 93), monthName, day, year);
			}
			return finalValidation;
		}
		System.out.println(withColor("Invalid date.", 91));
		return initialValidation;
	}
	
	public static boolean isMonthYearValid(String date) {
		String monthYearPattern = "(0?[1-9]|1[0-2])/(2[0-9][0-9][0-9])";
		boolean valid = date.matches(monthYearPattern);
		
		if (!valid) {
			System.out.println(withColor("Invalid month and year.", 91));
		}
		
		return valid;
	}
	
	public static String dateValidation(String prompt) {
		String date = null;
		boolean dateValid = false;
		while (!dateValid) {
			System.out.printf("%s (%s): ", prompt, withColor("MM/DD/YYYY", 92));
			date = input.nextLine();
			dateValid = isDateValid(date);
			System.out.println();
		}
		return formatDate(date);
	}
	
	public static String monthYearValidation(String prompt) {
		String monthYear = null;
		boolean dateValid = false;
		while (!dateValid) {
			System.out.printf("%s (%s): ", prompt, withColor("MM/YYYY", 92));
			monthYear = input.nextLine();
			dateValid = isMonthYearValid(monthYear);
			System.out.println();
		}
		return formatMonthYear(monthYear);
	}
	
	public static boolean isTimeValid(String time) { // time format: hh:mm(am/pm) without parenthesis and space
		String timePattern = "(0?[1-9]|1[0-2]):([0-5]?[0-9])([AaPp][Mm])";
		boolean timeValid = time.matches(timePattern);
		
		if (!timeValid) {
			System.out.println(withColor("Invalid time.", 91) + "\nExample: 10:30pm");
		}
		return timeValid;
	}

	public static boolean resolveOverlappingSchedule(String prompt, String time, List<String> schedules) {
		boolean timeValid = true;
		int timeInMinutes = militaryTime(time);
		// Check for overlapping time
		List<String> conflictSchedulesIndex = new ArrayList<>();
		for (String schedule : schedules) {
			String[] scheduleParts = schedule.split("~");
			int startTimeValue = strToInt(scheduleParts[4]);
			int endTimeValue = strToInt(scheduleParts[5]);
			if (timeInMinutes >= startTimeValue && timeInMinutes <= endTimeValue) {
				conflictSchedulesIndex.add(schedule);
			}
		}
		
		// Display prompt to user if overlapping schedules is found
		if (conflictSchedulesIndex.size() > 0) {
			System.out.print(withColor("\nFound some overlapping time with the following:", 93));
			displayDailySchedules(conflictSchedulesIndex);
			while (true) {
				System.out.printf("\nDo you want to change the %s time? (%s/%s): ", prompt, withColor("y", 92), withColor("n", 92));
				String isChangeTime = input.nextLine();
				if (isChangeTime.equalsIgnoreCase("y")) {
					timeValid = false; break;
				} else if (isChangeTime.equalsIgnoreCase("n")) {
					break;
				} else {
					System.out.println(withColor("Invalid Input.", 91));
				}
			}
		}
		return timeValid;
	}

	public static String startTimeValidation(String prompt, List<String> schedules) {
		boolean startTimeValid = false;
		String startTime = null;
		
		while (!startTimeValid) {
			System.out.printf("%s (%s(%s/%s)): ", prompt, withColor("hh:mm", 92), withColor("am", 92), withColor("pm", 92));
			startTime = input.nextLine();
			
			if (isTimeValid(startTime)) {
				startTimeValid = resolveOverlappingSchedule("start", startTime, schedules);
			}
			System.out.println();
		}
		
		return formatTime(startTime);
	}
	
	public static String endTimeValidation(String prompt, String startTime, int startTimeValue, List<String> schedules) {
		boolean endTimeValid = false;
		String endTime = null;
		
		while (!endTimeValid) {
			System.out.printf("%s (%s(%s/%s)): ", prompt, withColor("hh:mm", 92), withColor("am", 92), withColor("pm", 92));
			endTime = input.nextLine();
			if (endTime.isEmpty()) {
				endTime = startTime;
				endTimeValid = true;
			}
			
			endTimeValid = isTimeValid(endTime);
			if (endTimeValid && militaryTime(endTime) < startTimeValue) {
				endTimeValid = false;
				System.out.println(withColor("End time should be after start time.", 93));
			}
			else if (endTimeValid) {
				endTimeValid = resolveOverlappingSchedule("end", endTime, schedules);
			}
			System.out.println();
		}
		
		return formatTime(endTime);
	}
	
	public static String recurringSchedule(String date) {
		while (true) {
			System.out.printf("Repeat every year? (%s/%s): ", withColor("y", 92), withColor("n", 92));
			String isRepetition = input.nextLine();
			if (isRepetition.equalsIgnoreCase("y")) {
				date = date.substring(0, 5);
				System.out.println();
				return date;
			} else if (isRepetition.equalsIgnoreCase("n") || isRepetition.isEmpty()) {
				System.out.println();
				return date;
			} else {
				System.out.println(withColor("Invalid Input.\n", 91));
			}
		}
	}
	
	public static boolean deleteConfirmation(int scheduleIndex) {
		while (true) {
			System.out.printf("Certain to delete schedule at index %d? (%s/%s): ", scheduleIndex, withColor("y", 92), withColor("n", 92));
			String continueToDelete = input.nextLine();
			if (continueToDelete.equalsIgnoreCase("y")) {
				return true;
			} else if (continueToDelete.equalsIgnoreCase("n")) {
				return false;
			} else {
				System.out.println(withColor("Invalid Input.\n", 91));
			}
		}
	}
	
	
	// DISPLAY METHODS
	public static void displayDailySchedules(List<String> schedules, String date) {
		String header = """
\u001B[90m
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃                \u001B[92mSchedule/s for %s\u001B[90m                 ┃
┣━━━━━━━━━┳━━━━━━━━━┳━━━━━━━━━┳━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
┃  \u001B[92mIndex\u001B[90m  ┃  \u001B[92mStart\u001B[90m  ┃   \u001B[92mEnd\u001B[90m   ┃            \u001B[92mTitle\u001B[90m             ┃
				""";
		String content = """
┣━━━━━━━━━╋━━━━━━━━━╋━━━━━━━━━╋━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
┃    \u001B[97m%-4s\u001B[90m ┃ \u001B[97m%s\u001B[90m ┃ \u001B[97m%s\u001B[90m ┃ \u001B[97m%-28s\u001B[90m ┃
				""";
		String contentOverflow = """
┃         ┃         ┃         ┃ \u001B[97m%-28s\u001B[90m ┃
				""";
		
		String[] splittedDate = date.split("/");
		String textDate = String.format("%s %s, %s",
				getMonthName(Integer.parseInt(splittedDate[0])),
				splittedDate[1],
				splittedDate[2]);
		System.out.printf(header, textDate);
		
		for (int i = 0; i < schedules.size(); i++) {
			String[] parts = schedules.get(i).split("~");
			String title = parts[1];
			int maxTitleLength = 28;
			int startIndex = 0;
			String startTime = formatTime(parts[2]);
			String endTime = formatTime(parts[3]);
			
			while (startIndex < title.length()) {
				int end = Math.min(startIndex + maxTitleLength, title.length());
				int spaceIndex = title.lastIndexOf(" ", end);
				if (end < title.length() && spaceIndex > startIndex) {
					end = spaceIndex + 1;
				}
				
				String titleSegment = title.substring(startIndex, Math.min(end, title.length())).trim();
				if (startIndex == 0) {
					System.out.printf(content, i, startTime, endTime, titleSegment);
				} 
				else {
					System.out.printf(contentOverflow, titleSegment);
				}
				startIndex = end;
			}
		}
		System.out.printf("┗━━━━━━━━━┻━━━━━━━━━┻━━━━━━━━━┻━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛\u001B[0m");
	}

	public static void displayDailySchedules(List<String> schedules) {
		String header = """
\u001B[90m
┏━━━━━━━━━┳━━━━━━━━━┳━━━━━━━━━┳━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃  \u001B[92mIndex\u001B[90m  ┃  \u001B[92mStart\u001B[90m  ┃   \u001B[92mEnd\u001B[90m   ┃            \u001B[92mTitle\u001B[90m             ┃
				""";
		String content = """
┣━━━━━━━━━╋━━━━━━━━━╋━━━━━━━━━╋━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
┃    \u001B[97m%-4s\u001B[90m ┃ \u001B[97m%s\u001B[90m ┃ \u001B[97m%s\u001B[90m ┃ \u001B[97m%-28s\u001B[90m ┃
				""";
		String contentOverflow = """
┃         ┃         ┃         ┃ \u001B[97m%-28s\u001B[90m ┃
				""";
		
		System.out.print(header);
		
		for (int i = 0; i < schedules.size(); i++) {
			String[] parts = schedules.get(i).split("~");
			String title = parts[1];
			int maxTitleLength = 28;
			int startIndex = 0;
			String startTime = formatTime(parts[2]);
			String endTime = formatTime(parts[3]);
			
			while (startIndex < title.length()) {
				int end = Math.min(startIndex + maxTitleLength, title.length());
				int spaceIndex = title.lastIndexOf(" ", end);
				if (end < title.length() && spaceIndex > startIndex) {
					end = spaceIndex + 1;
				}
				
				String titleSegment = title.substring(startIndex, Math.min(end, title.length())).trim();
				if (startIndex == 0) {
					System.out.printf(content, i, startTime, endTime, titleSegment);
				} 
				else {
					System.out.printf(contentOverflow, titleSegment);
				}
				startIndex = end;
			}
		}
		System.out.printf("┗━━━━━━━━━┻━━━━━━━━━┻━━━━━━━━━┻━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛\u001B[0m");
	}
	
	public static void displayMonthlySchedules(HashSet<Integer> scheduleDates, HashSet<Integer> specialDates, String monthYear, String strMonth, String strYear) {
		int month = strToInt(strMonth);
		int year = strToInt(strYear);
		String monthName = getMonthName(month);
		int numOfDays = daysOfTheMonth(month, year);
		int firstDayOfTheWeek = determineDayOfTheWeek(month, 1, year);
		String header = """
\u001B[90m
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃             \u001B[92m%s %s\u001B[90m             ┃
┣━━━━┳━━━━┳━━━━┳━━━━┳━━━━┳━━━━┳━━━━┫
┃ \u001B[92mMo\u001B[90m ┃ \u001B[92mTu\u001B[90m ┃ \u001B[92mWe\u001B[90m ┃ \u001B[92mTh\u001B[90m ┃ \u001B[92mFr\u001B[90m ┃ \u001B[92mSa\u001B[90m ┃ \u001B[92mSu\u001B[90m ┃
┣━━━━╋━━━━╋━━━━╋━━━━╋━━━━╋━━━━╋━━━━┫
				""";
		String singleLine = "┃";
		String blankTile = "    ┃";
		String divider = "┣━━━━╋━━━━╋━━━━╋━━━━╋━━━━╋━━━━╋━━━━┫";
		
		// header
		System.out.printf(header, monthName, year);
		System.out.print(singleLine);
		
		// body
		int dayCounter = 0;
		for (int i = 1; i < firstDayOfTheWeek; i++) {
			System.out.print(blankTile);
			dayCounter++;
		}
		
		for (int day = 1; day <= numOfDays; day++) {
			if (dayCounter == 7) {
				System.out.println();
				System.out.println(divider);
				System.out.print(singleLine);
				dayCounter = 0;
			}
			
			int dayColorCode = 0;
			if (monthYear.equals(monthYearToday) && rawDateToday.getDayOfMonth() == day) {
				dayColorCode = 91;
			} else if (specialDates.contains(day)) {
				dayColorCode = 32;
			} else if (scheduleDates.contains(day)) {
				dayColorCode = 36;
			}
			System.out.printf(" %s ┃", withColor2(String.format("%2d", day), dayColorCode, 90));
			dayCounter++;
		}
		
		while (dayCounter != 7) {
			System.out.print(blankTile);
			dayCounter++;
		}
		
		System.out.println();
		System.out.println("┗━━━━┻━━━━┻━━━━┻━━━━┻━━━━┻━━━━┻━━━━┛\u001B[0m");
		
		System.out.printf(withColor("- Scheduled dates\n", 36));
		System.out.printf(withColor("- Current date\n", 91));
		System.out.printf(withColor("- Special Occasions\n", 32));		
	}
	
	public static void displayMatchedSchedules(List<String> schedules) {
		String header = """
\u001B[90m
┏━━━━━━━━━┳━━━━━━━━━━━━┳━━━━━━━━━┳━━━━━━━━━┳━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃  \u001B[92mIndex\u001B[90m  ┃    \u001B[92mDate\u001B[90m    ┃  \u001B[92mStart\u001B[90m  ┃   \u001B[92mEnd\u001B[90m   ┃            \u001B[92mTitle\u001B[90m             ┃
				""";
		String content = """
┣━━━━━━━━━╋━━━━━━━━━━━━╋━━━━━━━━━╋━━━━━━━━━╋━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
┃    \u001B[97m%-4s\u001B[90m ┃ \u001B[97m%-10s\u001B[90m ┃ \u001B[97m%s\u001B[90m ┃ \u001B[97m%s\u001B[90m ┃ \u001B[97m%-28s\u001B[90m ┃
				""";
		String contentOverflow = """
┃         ┃            ┃         ┃         ┃ \u001B[97m%-28s\u001B[90m ┃
				""";
		System.out.print(header);
		for (int i = 0; i < schedules.size(); i++) {
			String[] scheduleParts = schedules.get(i).split("~");

			String title = scheduleParts[1];
			int maxTitleLength = 28;
			int startIndex = 0;
			String date = scheduleParts[0];
			String startTime = formatTime(scheduleParts[2]);
			String endTime = formatTime(scheduleParts[3]);
			
			while (startIndex < title.length()) {
				int end = Math.min(startIndex + maxTitleLength, title.length());
				int spaceIndex = title.lastIndexOf(" ", end);
				if (end < title.length() && spaceIndex > startIndex) {
					end = spaceIndex + 1;
				}
				
				String titleSegment = title.substring(startIndex, Math.min(end, title.length())).trim();
				if (startIndex == 0) {
					System.out.printf(content, i, date, startTime, endTime, titleSegment);
				} 
				else {
					System.out.printf(contentOverflow, titleSegment);
				}
				startIndex = end;
			}
		}
		System.out.printf("┗━━━━━━━━━┻━━━━━━━━━━━━┻━━━━━━━━━┻━━━━━━━━━┻━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛\u001B[0m");
	}
}
