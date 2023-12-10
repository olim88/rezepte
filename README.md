# rezepte
mobile app for creating saveing and editing recipes

using kotlin and jetpack. I have create this android applications for recipe managment. 

## fetures
 - sync between devices with dropbox
 - loading recipe from websites
 - loading recipes from images
 - editing existing or createing own recipes
 - converting between units based of user prefrence
 - searching the users recipes

## releases
there are currently no built relases avalible
## build 
can be built using android stuido

## examlple xml saveing format (with coments

### search data XML
```xml
<searchData>  <!-- name used for the xml document containg data about search-->
	<list> <!-- the  list containing each recipe name and the data about them-->
		<entry> <!--entry for one of the recipes-->
			<name>
				example name
			</name>
			<servings>
				example servings
			</servings>
			<author>
				example author
			</author>
		</entry>		
	</list>
</searchData>
```
### main recipe XML
	
```xml
<recipe> <!--data structure containing whole recipe-->
	<data> <!-- data structure to whole infomation about the recipe that is not instructions or ingredients-->
		<name> <!--the name of the recipe-->
			example name
		</name>
		<author> <!--the author of the recipe-->
			example author
		</author>
		<servings> <!--the servings of the recipe-->
			example servings 
		</servings>
		<cookingSteps><!--the basic steps folowed in the recipe created by the user-->
			<list> <!--list of the steps-->
				<entry index="0"> <!--steps with index to keep order-->
					<time> <!--the length of the step-->
						example time
					</time>
					<cookingStage> <!--the main object used in the step-->
						oven
					</cookingStage>
					<cookingStepContainer> <!--what the step is done in e.g. bowl-->
						<type> <!--catagoriy of the item-->
							rectangleTin
						</type>
						<tinSize> <!--if needed the size of the cotiner in cm-->
							1.0
						</tinSize>
						<tinSizeTwo><!--if its a continer with 2 dimesions the second value of this also in cm-->
							1.0
						</tinSizeTwo>
					</cookingStepContainer>
					<cookingStepTemperature> <!--the temperature of the step if appicable-->
						<temperature> <!--oven temp in deg C-->
							0
						</temperature>
						<hobTemperature> <!--hob temp-->
							zero
						</hobTemperature>
						<isFan> <!--if the temperature is for a fan oven-->
							false
						</isFan>
					</cookingStepTemperature>
				</entry>
				<entry index="1">
					<time>
						example time
					</time>
					<cookingStage>
						oven
					</cookingStage>
					<cookingStepContainer>
						<type>
							dish
						</type>
						<tinVolume> <!--is applicable the volume of the continer in litres -->
							1.0
						</tinVolume>
					</cookingStepContainer>
					<cookingStepTemperature>
						<temperature>
							0
						</temperature>
						<hobTemperature>
							zero
						</hobTemperature>
						<isFan>
							false
						</isFan>
					</cookingStepTemperature>
				</entry>
			</list>
		</cookingSteps>
		<website><!--if recipe is linked to a website saves url of website-->
			example website
		</website>
		<notes><!--if the user input notes about the recipe saves them-->
			example notes
		</notes>
	</data>
	<ingredients> <!--the data structure containing the ingredients-->
		<list>
			<entry index="0"><!--an ingredeinted kept in order with index-->
				<value><!--what the ingredient says-->
					ingredient one
				</value>
			</entry>
			<entry index="1">
				<value>
					ingredient two
				</value>
			</entry>
			<entry index="2">
				<value>
					ingredient three
				</value>
			</entry>
		</list>
	</ingredients>
	<instructions> <!--data strucure containing the instructions of the recipe-->
		<list>
			<entry index="0"> <!--an instruction indexed-->
				<value><!--what the instruction says-->
					instruction one
				</value>
				<linkedStep> <!--when an instruction is conected to a step this saves the index of the step it is conted to -->
					0
				</linkedStep>
			</entry>
			<entry index="1">
				<value>
					instruction two
				</value>
			</entry>
		</list>
	</instructions>
</recipe>
```
