import os
import subprocess
from datetime import datetime, timedelta

def run_command(command):
    result = subprocess.run(command, shell=True, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Error running: {command}\n{result.stderr}")
    return result.stdout.strip()

def get_commit_message(filepath):
    filename = os.path.basename(filepath)
    if filename == 'build.gradle':
        return "Configure project dependencies and build settings"
    if filename == 'AndroidManifest.xml':
        return "Declare app components, permissions, and metadata"
    if filename == 'MainActivity.java':
        return "Implement main activity logic and WebView integration"
    if filename.endswith('.java'):
        return f"Implement logic for {filename}"
    if filename.endswith('.xml'):
        if 'layout' in filepath:
            return f"Define UI layout for {filename}"
        return f"Configure resources in {filename}"
    if filename == '.gitignore':
        return "Add .gitignore to exclude build artifacts and IDE files"
    if filename == 'gradle.properties':
        return "Configure Gradle project properties"
    if filename == 'settings.gradle':
        return "Configure Gradle project settings"
    return f"Add {filepath}"

def main():
    base_date = datetime(2024, 4, 1, 10, 0, 0)

    # Get all untracked files
    files = run_command("git ls-files --others --exclude-standard").split('\n')
    files = [f for f in files if f and f != 'backdate_commits.py']

    # Get file info: (path, creation_time)
    file_info = []
    for f in files:
        if not os.path.exists(f): continue
        ctime = datetime.fromtimestamp(os.path.getctime(f))
        # Ensure it's not before the base date requested by user
        if ctime < base_date:
            ctime = base_date
        file_info.append((f, ctime))

    # Sort by creation time to create a logical history
    file_info.sort(key=lambda x: x[1])

    # To avoid exact same timestamps for multiple files at the "floor"
    current_offset = 0

    for i, (filepath, ctime) in enumerate(file_info):
        # Add a small offset (e.g. 1 second per file) if they are at the floor
        if ctime == base_date:
            ctime = ctime + timedelta(seconds=i)

        date_str = ctime.strftime('%Y-%m-%d %H:%M:%S')
        message = get_commit_message(filepath)

        print(f"Committing {filepath} with date {date_str}...")

        run_command(f'git add "{filepath}"')
        # Use GIT_AUTHOR_DATE and GIT_COMMITTER_DATE for backdating
        env = os.environ.copy()
        env['GIT_AUTHOR_DATE'] = date_str
        env['GIT_COMMITTER_DATE'] = date_str

        subprocess.run(['git', 'commit', '-m', message], env=env, capture_output=True)

    print("All files committed.")

if __name__ == "__main__":
    main()
